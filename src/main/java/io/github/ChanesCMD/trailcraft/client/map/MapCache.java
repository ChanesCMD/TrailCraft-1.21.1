package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Память карты на клиенте: цвета исследованных чанков (16×16, формат NativeImage, 0 — неизвестно). Хранится по
 * регионам 32×32 чанка в .minecraft/trailcraft/maps/<мир>/<измерение>/r.X.Z.dat; регион читается с диска при первом
 * обращении, изменённые сохраняются раз в минуту (в отдельном потоке, чтобы не было фризов) и при выходе из мира.
 */
public final class MapCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int REGION = 32, FORMAT = 1;
    private static final Map<ResourceLocation, Map<Long, Region>> DIMENSIONS = new HashMap<>();
    private static Path root;
    /** Запись регионов на диск — по очереди в одном фоновом потоке. Массивы цветов после записи в кэш не меняются. */
    private static final ExecutorService SAVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TrailCraft map saver");
        thread.setDaemon(true);
        return thread;
    });

    private static final class Region {
        final Map<Integer, int[]> chunks = new HashMap<>();
        boolean dirty;
    }

    /** Вход в мир: память этого мира или сервера. */
    public static void open() {
        close();
        root = Minecraft.getInstance().gameDirectory.toPath().resolve("trailcraft").resolve("maps").resolve(worldId());
    }

    /** Выход из мира: сохранить изменения и забыть. */
    public static void close() {
        save();
        try {
            SAVER.submit(() -> {}).get(); // дождаться записи, чтобы при следующем входе прочитать свежие файлы
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.warn("Map saving failed", e);
        }
        DIMENSIONS.clear();
        root = null;
    }

    public static boolean hasChunk(ResourceLocation dimension, int cx, int cz) {
        return chunk(dimension, cx, cz) != null;
    }

    /** Цвета чанка (256, по строкам с севера) или null, если чанк не исследован. */
    public static int[] chunk(ResourceLocation dimension, int cx, int cz) {
        Region region = region(dimension, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
        return region == null ? null : region.chunks.get(index(cx, cz));
    }

    public static void putChunk(ResourceLocation dimension, int cx, int cz, int[] colors) {
        Region region = region(dimension, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
        if (region != null) {
            region.chunks.put(index(cx, cz), colors);
            region.dirty = true;
            MapTiles.invalidate(dimension, Math.floorDiv(cx, MapTiles.CHUNKS), Math.floorDiv(cz, MapTiles.CHUNKS));
        }
    }

    /** Сохраняет изменённые регионы в фоне (копия списка чанков, сами массивы общие). */
    public static void save() {
        if (root == null) {
            return;
        }
        for (Map.Entry<ResourceLocation, Map<Long, Region>> dimension : DIMENSIONS.entrySet()) {
            for (Map.Entry<Long, Region> entry : dimension.getValue().entrySet()) {
                Region region = entry.getValue();
                if (region.dirty) {
                    region.dirty = false;
                    Path file = file(dimension.getKey(), (int) (entry.getKey() >> 32), (int) (long) entry.getKey());
                    Map<Integer, int[]> chunks = new HashMap<>(region.chunks);
                    SAVER.execute(() -> write(file, chunks));
                }
            }
        }
    }

    private static int index(int cx, int cz) {
        return Math.floorMod(cz, REGION) * REGION + Math.floorMod(cx, REGION);
    }

    private static Region region(ResourceLocation dimension, int rx, int rz) {
        if (root == null) {
            return null;
        }
        long key = (long) rx << 32 | (rz & 0xFFFFFFFFL);
        return DIMENSIONS.computeIfAbsent(dimension, d -> new HashMap<>()).computeIfAbsent(key, k -> read(file(dimension, rx, rz)));
    }

    private static Path file(ResourceLocation dimension, int rx, int rz) {
        return root.resolve(sanitize(dimension.getNamespace() + "_" + dimension.getPath())).resolve("r." + rx + "." + rz + ".dat");
    }

    private static Region read(Path file) {
        Region region = new Region();
        if (!Files.isRegularFile(file)) {
            return region;
        }
        try (InputStream raw = Files.newInputStream(file); DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            if (in.readInt() != FORMAT) {
                return region;
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int index = in.readShort();
                int[] colors = new int[256];
                for (int j = 0; j < 256; j++) {
                    colors[j] = in.readInt();
                }
                region.chunks.put(index, colors);
            }
        } catch (IOException e) {
            LOGGER.warn("Can't read map region {}", file, e);
        }
        return region;
    }

    private static void write(Path file, Map<Integer, int[]> chunks) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream raw = Files.newOutputStream(file); DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(raw)))) {
                out.writeInt(FORMAT);
                out.writeInt(chunks.size());
                for (Map.Entry<Integer, int[]> chunk : chunks.entrySet()) {
                    out.writeShort(chunk.getKey());
                    for (int color : chunk.getValue()) {
                        out.writeInt(color);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Can't save map region {}", file, e);
        }
    }

    /** Одиночный мир — по имени папки сохранения, сервер — по адресу. */
    private static String worldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            Path world = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return sanitize("sp_" + world.getFileName());
        }
        ServerData server = mc.getCurrentServer();
        return sanitize("mp_" + (server != null ? server.ip : "unknown"));
    }

    private static String sanitize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private MapCache() {}
}

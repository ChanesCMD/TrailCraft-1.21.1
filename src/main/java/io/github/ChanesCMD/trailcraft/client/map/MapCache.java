package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Память карты на клиенте: цвета исследованных чанков (16×16, формат NativeImage, 0 — неизвестно). Хранится по
 * регионам 32×32 чанка в .minecraft/trailcraft/maps/<мир>/<измерение>/r.X.Z.dat. Цвета чанка — номера в палитре
 * региона (2 байта на точку вместо 4). Регион читается с диска в фоне при первом обращении (пока читается, его чанки
 * считаются известными, а на карте пусто); изменённые сохраняются раз в минуту и при выходе из мира, тоже в фоне.
 * Чтение и запись идут по очереди в одном потоке, поэтому файл никогда не читается посреди записи.
 */
public final class MapCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Формат 1 — полные цвета (int на точку), формат 2 — палитра региона и номера в ней (char на точку). */
    private static final int REGION = 32, FORMAT = 2, MAX_PALETTE = 1 << 16;
    private static final Map<ResourceLocation, Map<Long, Region>> DIMENSIONS = new HashMap<>();
    /** Регионы, которые ещё читаются с диска. */
    private static final List<Region> LOADING = new ArrayList<>();
    private static Path root;
    /** Чтение и запись регионов — по очереди в одном фоновом потоке. Массивы чанков после записи в кэш не меняются. */
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TrailCraft map IO");
        thread.setDaemon(true);
        return thread;
    });

    /** Прочитанное с диска: палитра и чанки (номера в палитре). */
    private record Data(int[] palette, Int2ObjectMap<char[]> chunks) {}

    private static final class Region {
        final ResourceLocation dimension;
        final int rx, rz;
        final Int2ObjectMap<char[]> chunks = new Int2ObjectOpenHashMap<>();
        final IntArrayList palette = new IntArrayList();
        final Int2IntOpenHashMap paletteIndex = new Int2IntOpenHashMap();
        boolean dirty, loading = true;
        /** Результат фонового чтения; применяется в главном потоке (applyLoaded). */
        volatile Data loaded;

        Region(ResourceLocation dimension, int rx, int rz) {
            this.dimension = dimension;
            this.rx = rx;
            this.rz = rz;
            paletteIndex.defaultReturnValue(-1);
        }

        char[] encode(int[] colors) {
            char[] indices = new char[256];
            for (int i = 0; i < 256; i++) {
                indices[i] = (char) indexOf(colors[i]);
            }
            return indices;
        }

        int[] decode(char[] indices) {
            int[] colors = new int[256];
            for (int i = 0; i < 256; i++) {
                colors[i] = palette.getInt(indices[i]);
            }
            return colors;
        }

        private int indexOf(int color) {
            int index = paletteIndex.get(color);
            if (index >= 0) {
                return index;
            }
            if (palette.size() < MAX_PALETTE) {
                paletteIndex.put(color, palette.size());
                palette.add(color);
                return palette.size() - 1;
            }
            return nearest(color); // палитра полна (на практике в регионе в разы меньше цветов) — ближайший цвет
        }

        private int nearest(int color) {
            int best = 0, bestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < palette.size(); i++) {
                int other = palette.getInt(i), distance = 0;
                for (int shift = 0; shift < 24; shift += 8) {
                    int d = (color >> shift & 0xFF) - (other >> shift & 0xFF);
                    distance += d * d;
                }
                if (distance < bestDistance) {
                    best = i;
                    bestDistance = distance;
                }
            }
            return best;
        }

        /** Прочитанное с диска добавляется к тому, что успели снять, пока регион читался (снятое новее). */
        boolean applyLoaded() {
            Data data = loaded;
            if (!loading || data == null) {
                return false;
            }
            loading = false;
            loaded = null;
            if (chunks.isEmpty() && palette.isEmpty()) {
                palette.addElements(0, data.palette());
                for (int i = 0; i < data.palette().length; i++) {
                    paletteIndex.putIfAbsent(data.palette()[i], i);
                }
                chunks.putAll(data.chunks());
            } else {
                for (Int2ObjectMap.Entry<char[]> entry : data.chunks().int2ObjectEntrySet()) {
                    if (!chunks.containsKey(entry.getIntKey())) {
                        int[] colors = new int[256];
                        for (int i = 0; i < 256; i++) {
                            colors[i] = data.palette()[entry.getValue()[i]];
                        }
                        chunks.put(entry.getIntKey(), encode(colors));
                    }
                }
            }
            int tiles = REGION / MapTiles.CHUNKS;
            for (int tz = 0; tz < tiles; tz++) {
                for (int tx = 0; tx < tiles; tx++) {
                    MapTiles.invalidate(dimension, rx * tiles + tx, rz * tiles + tz);
                }
            }
            return true;
        }
    }

    /** Вход в мир: память этого мира или сервера. */
    public static void open() {
        close();
        root = Minecraft.getInstance().gameDirectory.toPath().resolve("trailcraft").resolve("maps").resolve(worldId());
    }

    /** Выход из мира: дочитать начатое, сохранить изменения и забыть. */
    public static void close() {
        waitIO();
        tick();
        save();
        waitIO(); // дождаться записи, чтобы при следующем входе прочитать свежие файлы
        DIMENSIONS.clear();
        LOADING.clear();
        root = null;
    }

    private static void waitIO() {
        try {
            IO.submit(() -> {}).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.warn("Map IO failed", e);
        }
    }

    /** Каждый тик: прочитанные в фоне регионы попадают в память. */
    public static void tick() {
        LOADING.removeIf(Region::applyLoaded);
    }

    /** Исследован ли чанк. Пока регион читается с диска, его чанки считаются исследованными. */
    public static boolean hasChunk(ResourceLocation dimension, int cx, int cz) {
        Region region = region(dimension, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
        return region != null && (region.loading || region.chunks.containsKey(index(cx, cz)));
    }

    /** Цвета чанка (256, по строкам с севера) или null, если чанк не исследован (или регион ещё читается). */
    public static int[] chunk(ResourceLocation dimension, int cx, int cz) {
        Region region = region(dimension, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
        char[] indices = region == null ? null : region.chunks.get(index(cx, cz));
        return indices == null ? null : region.decode(indices);
    }

    public static void putChunk(ResourceLocation dimension, int cx, int cz, int[] colors) {
        Region region = region(dimension, Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
        if (region != null) {
            region.chunks.put(index(cx, cz), region.encode(colors));
            region.dirty = true;
            MapTiles.invalidate(dimension, Math.floorDiv(cx, MapTiles.CHUNKS), Math.floorDiv(cz, MapTiles.CHUNKS));
        }
    }

    /** Сохраняет изменённые регионы в фоне (копии палитры и списка чанков, сами массивы общие). */
    public static void save() {
        if (root == null) {
            return;
        }
        for (Map<Long, Region> regions : DIMENSIONS.values()) {
            for (Region region : regions.values()) {
                // Ещё не прочитанный регион не пишем: иначе файл затрётся тем немногим, что успели снять
                if (region.dirty && !region.loading) {
                    region.dirty = false;
                    Path file = file(region.dimension, region.rx, region.rz);
                    int[] palette = region.palette.toIntArray();
                    Int2ObjectMap<char[]> chunks = new Int2ObjectOpenHashMap<>(region.chunks);
                    IO.execute(() -> write(file, palette, chunks));
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
        Map<Long, Region> regions = DIMENSIONS.computeIfAbsent(dimension, d -> new HashMap<>());
        Region region = regions.get(key);
        if (region == null) {
            Region created = new Region(dimension, rx, rz);
            Path file = file(dimension, rx, rz);
            IO.execute(() -> created.loaded = read(file));
            regions.put(key, created);
            LOADING.add(created);
            region = created;
        }
        return region;
    }

    private static Path file(ResourceLocation dimension, int rx, int rz) {
        return root.resolve(sanitize(dimension.getNamespace() + "_" + dimension.getPath())).resolve("r." + rx + "." + rz + ".dat");
    }

    /** Читает регион (в фоновом потоке). Старый формат 1 переводится в палитру; на диск он перепишется при сохранении. */
    private static Data read(Path file) {
        Int2ObjectMap<char[]> chunks = new Int2ObjectOpenHashMap<>();
        if (!Files.isRegularFile(file)) {
            return new Data(new int[0], chunks);
        }
        try (InputStream raw = Files.newInputStream(file); DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            int format = in.readInt();
            if (format == 2) {
                int[] palette = new int[in.readInt()];
                for (int i = 0; i < palette.length; i++) {
                    palette[i] = in.readInt();
                }
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    int index = in.readShort();
                    char[] indices = new char[256];
                    for (int j = 0; j < 256; j++) {
                        indices[j] = in.readChar();
                    }
                    chunks.put(index, indices);
                }
                return new Data(palette, chunks);
            }
            if (format == 1) {
                IntArrayList palette = new IntArrayList();
                Int2IntOpenHashMap paletteIndex = new Int2IntOpenHashMap();
                paletteIndex.defaultReturnValue(-1);
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    int index = in.readShort();
                    char[] indices = new char[256];
                    for (int j = 0; j < 256; j++) {
                        int color = in.readInt();
                        int k = paletteIndex.get(color);
                        if (k < 0 && palette.size() < MAX_PALETTE) {
                            k = palette.size();
                            paletteIndex.put(color, k);
                            palette.add(color);
                        }
                        indices[j] = (char) Math.max(k, 0);
                    }
                    chunks.put(index, indices);
                }
                return new Data(palette.toIntArray(), chunks);
            }
        } catch (IOException e) {
            LOGGER.warn("Can't read map region {}", file, e);
        }
        return new Data(new int[0], new Int2ObjectOpenHashMap<>());
    }

    private static void write(Path file, int[] palette, Int2ObjectMap<char[]> chunks) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream raw = Files.newOutputStream(file); DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(raw)))) {
                out.writeInt(FORMAT);
                out.writeInt(palette.length);
                for (int color : palette) {
                    out.writeInt(color);
                }
                out.writeInt(chunks.size());
                for (Int2ObjectMap.Entry<char[]> chunk : chunks.int2ObjectEntrySet()) {
                    out.writeShort(chunk.getIntKey());
                    for (char index : chunk.getValue()) {
                        out.writeChar(index);
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

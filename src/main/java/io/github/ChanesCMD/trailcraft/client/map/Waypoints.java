package io.github.ChanesCMD.trailcraft.client.map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Путевые точки игрока (пока видны только на полноэкранной карте). Хранятся на клиенте рядом с памятью карты:
 * .minecraft/trailcraft/maps/<мир>/waypoints.json, у каждого мира или сервера свои; у точки есть измерение.
 */
public final class Waypoints {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Цвета меток на выбор (RGB). */
    public static final int[] COLORS = {0xFF5555, 0xFFAA33, 0xFFE14D, 0x55D655, 0x55D6D6, 0x5588FF, 0xAA66FF, 0xFFFFFF};
    private static final Codec<List<Waypoint>> LIST = Waypoint.CODEC.listOf();
    private static final List<Waypoint> ALL = new ArrayList<>();

    /** Точка: название, измерение, координаты (y — высота поверхности при создании) и цвет. */
    public record Waypoint(String name, ResourceLocation dimension, int x, int y, int z, int color) {
        public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Waypoint::name),
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(Waypoint::dimension),
                Codec.INT.fieldOf("x").forGetter(Waypoint::x),
                Codec.INT.fieldOf("y").forGetter(Waypoint::y),
                Codec.INT.fieldOf("z").forGetter(Waypoint::z),
                Codec.INT.fieldOf("color").forGetter(Waypoint::color)).apply(instance, Waypoint::new));
    }

    /** Точки в измерении (новый список; сами точки неизменяемые). */
    public static List<Waypoint> inDimension(ResourceLocation dimension) {
        return ALL.stream().filter(w -> w.dimension().equals(dimension)).toList();
    }

    public static int count() {
        return ALL.size();
    }

    public static void add(Waypoint waypoint) {
        ALL.add(waypoint);
        save();
    }

    public static void replace(Waypoint old, Waypoint changed) {
        int index = ALL.indexOf(old);
        if (index >= 0) {
            ALL.set(index, changed);
            save();
        }
    }

    public static void remove(Waypoint waypoint) {
        if (ALL.remove(waypoint)) {
            save();
        }
    }

    /** Вход в мир: точки этого мира или сервера (MapCache.open() уже вызван). */
    public static void open() {
        ALL.clear();
        Path file = file();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            LIST.parse(JsonOps.INSTANCE, json).resultOrPartial(error -> LOGGER.warn("Bad waypoints file {}: {}", file, error))
                    .ifPresent(ALL::addAll);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Can't read waypoints {}", file, e);
        }
    }

    public static void close() {
        ALL.clear();
    }

    private static void save() {
        Path file = file();
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            JsonElement json = LIST.encodeStart(JsonOps.INSTANCE, ALL).getOrThrow();
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Can't save waypoints {}", file, e);
        }
    }

    private static Path file() {
        Path root = MapCache.worldRoot();
        return root == null ? null : root.resolve("waypoints.json");
    }

    private Waypoints() {}
}

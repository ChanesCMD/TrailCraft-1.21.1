package io.github.ChanesCMD.trailcraft.client;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** Настройки клиента: файл config/trailcraft-client.toml. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Дополнительные каналы чата сервера, которые игрок у себя скрыл (обязательные скрыть нельзя, ТЗ 5.3). */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_CHANNELS = BUILDER
            .comment("Extra server chat channels hidden on this client.")
            .defineListAllowEmpty("hiddenChannels", List.of(), () -> "trading", value -> value instanceof String);

    /** Размер миникарты в пикселях интерфейса (квадрат); по умолчанию 66, один пиксель — один блок. */
    public static final int MINIMAP_MIN = 40, MINIMAP_MAX = 160, MINIMAP_DEFAULT = 66;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE = BUILDER
            .comment("Minimap size in GUI pixels.")
            .defineInRange("minimapSize", MINIMAP_DEFAULT, MINIMAP_MIN, MINIMAP_MAX);

    /** Угол экрана, в котором стоят миникарта, дата и подписи под ней. */
    public enum Corner {
        TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT;

        public boolean left() {
            return this == TOP_LEFT || this == BOTTOM_LEFT;
        }

        public boolean bottom() {
            return this == BOTTOM_RIGHT || this == BOTTOM_LEFT;
        }
    }

    /** Что показывать из календаря рядом с миникартой (нужен календарь в рюкзаке). */
    public enum DateMode {
        FULL, DATE, TIME, OFF;

        public boolean showDate() {
            return this == FULL || this == DATE;
        }

        public boolean showTime() {
            return this == FULL || this == TIME;
        }
    }

    public static final ModConfigSpec.EnumValue<DateMode> DATE_MODE = BUILDER
            .comment("What to show from the calendar: FULL (date and time), DATE, TIME or OFF.")
            .defineEnum("dateMode", DateMode.FULL);
    public static final ModConfigSpec.BooleanValue MINIMAP_ENABLED = BUILDER
            .comment("Show the minimap (the date and captions stay).")
            .define("minimapEnabled", true);
    public static final ModConfigSpec.EnumValue<Corner> MINIMAP_CORNER = BUILDER
            .comment("Screen corner of the minimap, the date and the captions.")
            .defineEnum("minimapCorner", Corner.TOP_RIGHT);
    public static final ModConfigSpec.BooleanValue MINIMAP_ROTATE = BUILDER
            .comment("Rotate the minimap so that it always points where the player looks.")
            .define("minimapRotate", false);
    public static final ModConfigSpec.BooleanValue MINIMAP_BIOME = BUILDER
            .comment("Show the biome name next to the minimap.")
            .define("minimapBiome", true);
    public static final ModConfigSpec.BooleanValue MINIMAP_COORDS = BUILDER
            .comment("Show the player's coordinates next to the minimap.")
            .define("minimapCoords", false);
    public static final ModConfigSpec.IntValue MINIMAP_OPACITY = BUILDER
            .comment("Minimap opacity in percent.")
            .defineInRange("minimapOpacity", 100, 20, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int minimapSize() {
        return SPEC.isLoaded() ? MINIMAP_SIZE.get() : MINIMAP_DEFAULT;
    }

    public static void setMinimapSize(int size) {
        MINIMAP_SIZE.set(Math.max(MINIMAP_MIN, Math.min(MINIMAP_MAX, size)));
    }

    public static DateMode dateMode() {
        return SPEC.isLoaded() ? DATE_MODE.get() : DateMode.FULL;
    }

    public static boolean minimapEnabled() {
        return !SPEC.isLoaded() || MINIMAP_ENABLED.get();
    }

    public static Corner minimapCorner() {
        return SPEC.isLoaded() ? MINIMAP_CORNER.get() : Corner.TOP_RIGHT;
    }

    public static boolean minimapRotate() {
        return SPEC.isLoaded() && MINIMAP_ROTATE.get();
    }

    public static boolean minimapBiome() {
        return !SPEC.isLoaded() || MINIMAP_BIOME.get();
    }

    public static boolean minimapCoords() {
        return SPEC.isLoaded() && MINIMAP_COORDS.get();
    }

    /** Непрозрачность миникарты от 0,2 до 1. */
    public static float minimapOpacity() {
        return SPEC.isLoaded() ? MINIMAP_OPACITY.get() / 100f : 1f;
    }

    /** Все настройки миникарты и даты — по умолчанию. */
    public static void resetMinimap() {
        MINIMAP_SIZE.set(MINIMAP_DEFAULT);
        DATE_MODE.set(DATE_MODE.getDefault());
        MINIMAP_ENABLED.set(MINIMAP_ENABLED.getDefault());
        MINIMAP_CORNER.set(MINIMAP_CORNER.getDefault());
        MINIMAP_ROTATE.set(MINIMAP_ROTATE.getDefault());
        MINIMAP_BIOME.set(MINIMAP_BIOME.getDefault());
        MINIMAP_COORDS.set(MINIMAP_COORDS.getDefault());
        MINIMAP_OPACITY.set(MINIMAP_OPACITY.getDefault());
    }

    public static boolean isHidden(String channel) {
        return SPEC.isLoaded() && HIDDEN_CHANNELS.get().contains(channel);
    }

    public static void setHidden(String channel, boolean hidden) {
        List<String> list = new ArrayList<>(HIDDEN_CHANNELS.get());
        list.remove(channel);
        if (hidden) {
            list.add(channel);
        }
        HIDDEN_CHANNELS.set(list);
        SPEC.save();
    }

    private ClientConfig() {}
}

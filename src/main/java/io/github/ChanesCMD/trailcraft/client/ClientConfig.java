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

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int minimapSize() {
        return SPEC.isLoaded() ? MINIMAP_SIZE.get() : MINIMAP_DEFAULT;
    }

    public static void setMinimapSize(int size) {
        MINIMAP_SIZE.set(Math.max(MINIMAP_MIN, Math.min(MINIMAP_MAX, size)));
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

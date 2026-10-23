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

    public static final ModConfigSpec SPEC = BUILDER.build();

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

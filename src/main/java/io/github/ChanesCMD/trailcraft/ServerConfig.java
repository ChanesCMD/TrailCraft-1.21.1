package io.github.ChanesCMD.trailcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Серверные настройки мода: файл &lt;мир&gt;/serverconfig/trailcraft-server.toml.
 * Хранятся в папке мира, поэтому на сервере их меняет только администратор; клиентам значения приходят автоматически.
 */
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> DAYS_PER_SEASON;
    public static final ModConfigSpec.ConfigValue<Integer> DAY_LENGTH_MINUTES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_CHANNELS;
    public static final ModConfigSpec.BooleanValue VOICE_REQUIRES_DEVICE;

    /** Обязательные каналы чата: удалить нельзя (ТЗ 5.2). Дополнительные — в EXTRA_CHANNELS. */
    public static final List<String> REQUIRED_CHANNELS = List.of("global", "party", "country", "city");

    static {
        BUILDER.comment("Calendar: 1 year = 4 seasons").push("calendar");
        DAYS_PER_SEASON = BUILDER
                .comment("Length of one season in days. Allowed: 12 or 24.")
                .define("daysPerSeason", 12, v -> v instanceof Integer i && (i == 12 || i == 24));
        DAY_LENGTH_MINUTES = BUILDER
                .comment("Length of one full day (day + night) in real minutes. Allowed: 24 or 48. Vanilla is 20.")
                .define("dayLengthMinutes", 24, v -> v instanceof Integer i && (i == 24 || i == 48));
        BUILDER.pop();

        BUILDER.comment("Chat and voice chat").push("chat");
        EXTRA_CHANNELS = BUILDER
                .comment("Extra text chat channels besides the required global, party, country and city. Owners can add and remove them.")
                .defineListAllowEmpty("extraChannels", List.of("trading"), () -> "trading", ServerConfig::validChannel);
        VOICE_REQUIRES_DEVICE = BUILDER
                .comment("Without a communication device only the local voice chat is available.")
                .define("voiceRequiresDevice", true);
        BUILDER.pop();
    }

    /** Имя канала: латиница, цифры, _ и -, до 16 символов, не совпадает с обязательными. */
    public static boolean validChannel(Object value) {
        return value instanceof String name && name.matches("[a-z0-9_-]{1,16}") && !REQUIRED_CHANNELS.contains(name);
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Значение длины сезона, или 12, если конфиг ещё не загружен (до входа в мир). */
    public static int daysPerSeason() {
        return SPEC.isLoaded() ? DAYS_PER_SEASON.get() : 12;
    }

    /** Длина суток в минутах, или 0, если конфиг ещё не загружен (тогда время не трогаем). */
    public static int dayLengthMinutes() {
        return SPEC.isLoaded() ? DAY_LENGTH_MINUTES.get() : 0;
    }

    /** Дополнительные каналы чата в порядке конфига. */
    public static List<String> extraChannels() {
        return SPEC.isLoaded() ? List.copyOf(EXTRA_CHANNELS.get()) : List.of("trading");
    }

    /** Без устройства связи доступен только локальный голосовой чат. */
    public static boolean voiceRequiresDevice() {
        return !SPEC.isLoaded() || VOICE_REQUIRES_DEVICE.get();
    }
}

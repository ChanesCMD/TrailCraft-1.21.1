package io.github.ChanesCMD.trailcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Серверные настройки мода: файл &lt;мир&gt;/serverconfig/trailcraft-server.toml.
 * Хранятся в папке мира, поэтому на сервере их меняет только администратор; клиентам значения приходят автоматически.
 */
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> DAYS_PER_SEASON;
    public static final ModConfigSpec.ConfigValue<Integer> DAY_LENGTH_MINUTES;

    static {
        BUILDER.comment("Calendar: 1 year = 4 seasons").push("calendar");
        DAYS_PER_SEASON = BUILDER
                .comment("Length of one season in days. Allowed: 12 or 24.")
                .define("daysPerSeason", 12, v -> v instanceof Integer i && (i == 12 || i == 24));
        DAY_LENGTH_MINUTES = BUILDER
                .comment("Length of one full day (day + night) in real minutes. Allowed: 24 or 48. Vanilla is 20.")
                .define("dayLengthMinutes", 24, v -> v instanceof Integer i && (i == 24 || i == 48));
        BUILDER.pop();
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
}

package io.github.ChanesCMD.trailcraft.calendar;

/**
 * Календарь мира: 1 год = 4 сезона, сезон = 12 или 24 дня (серверный конфиг).
 * Считается от времени суток мира (dayTime): 24000 тиков = одни сутки, 0 тиков = 06:00; новый день начинается в 00:00
 * (dayTime 18000, 42000, …) — то есть дата считается по dayTime + {@link #MIDNIGHT_OFFSET}.
 */
public record Calendar(long year, Season season, int dayOfSeason, int daysPerSeason, int hour, int minute) {
    public static final int TICKS_PER_DAY = 24000;
    /** Сдвиг dayTime до полуночи: dayTime 0 — это 06:00, полночь на 6000 тиков раньше. */
    public static final int MIDNIGHT_OFFSET = 6000;
    /** Год, с которого начинается календарь нового мира (1-й день весны). */
    public static final long START_YEAR = 620;

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER;

        public String translationKey() {
            return "season.trailcraft." + name().toLowerCase();
        }
    }

    public static Calendar of(long dayTime, int daysPerSeason) {
        long t = Math.max(0, dayTime + MIDNIGHT_OFFSET);
        long day = t / TICKS_PER_DAY;
        long daysPerYear = 4L * daysPerSeason;
        long year = day / daysPerYear + START_YEAR;
        Season season = Season.values()[(int) ((day % daysPerYear) / daysPerSeason)];
        int dayOfSeason = (int) (day % daysPerSeason) + 1;
        long inDay = t % TICKS_PER_DAY;
        int hour = (int) (inDay / 1000);
        int minute = (int) (inDay % 1000 * 60 / 1000);
        return new Calendar(year, season, dayOfSeason, daysPerSeason, hour, minute);
    }

    public String time() {
        return String.format("%02d:%02d", hour, minute);
    }
}

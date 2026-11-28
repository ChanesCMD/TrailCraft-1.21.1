package io.github.ChanesCMD.trailcraft.client.nightsky;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Редкая арбузная луна: первое полнолуние лета и два дня до/после него. Кот появляется на само
 * полнолуние, остальные дни видны арбузные фазы. Раз в игровой год — достаточно редко.
 *
 * Другие варианты, если захочется: 3 августа (Всемирный день арбуза, реальная дата); 13-е полнолуние
 * («голубая луна»); августовское реальное полнолуние (тогда в августе оно перекрывает сырное).
 */
public final class WatermelonMoon {
    private static final long TICKS_PER_DAY = 24_000L;
    /** Полнолуния повторяются каждые 8 игровых суток. */
    private static final long LUNATION = 8L * TICKS_PER_DAY;
    /** За сколько дней до/после полнолуния уже показывать арбузные фазы. */
    private static final int WINDOW = 2;

    private WatermelonMoon() {}

    public static boolean isActive(ClientLevel level) {
        if (level == null) {
            return false;
        }
        long time = level.getDayTime();
        int phase = level.getMoonPhase() & 7;
        // ближайшее полнолуние: прошедшее (phase 0..2) или будущее (phase 6..7)
        long full;
        if (phase <= WINDOW) {
            full = time - phase * TICKS_PER_DAY;
        } else if (phase >= 8 - WINDOW) {
            full = time + (8 - phase) * TICKS_PER_DAY;
        } else {
            return false;
        }
        if (Seasons.of(full) != Calendar.Season.SUMMER) {
            return false;
        }
        // первое полнолуние лета: предыдущее полнолуние было ещё не летом
        return Seasons.of(full - LUNATION) != Calendar.Season.SUMMER;
    }
}

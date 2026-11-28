package io.github.ChanesCMD.trailcraft.client.nightsky;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Редкая арбузная луна: первое полнолуние лета и по четыре дня до/после него. Так видны все фазы:
 * до полнолуния 4, 5, 6, 7, в полнолуние полная (кот), после — 1, 2, 3, 4. Раз в игровой год.
 *
 * Другие варианты, если захочется: 3 августа (Всемирный день арбуза, реальная дата); 13-е полнолуние
 * («голубая луна»); августовское реальное полнолуние (тогда в августе оно перекрывает сырное).
 */
public final class WatermelonMoon {
    private static final long TICKS_PER_DAY = 24_000L;
    /** Полнолуния повторяются каждые 8 игровых суток. */
    private static final long LUNATION = 8L * TICKS_PER_DAY;
    /** За сколько дней до/после полнолуния показывать арбузные фазы (весь цикл, включая новолуние). */
    private static final int WINDOW = 4;

    private WatermelonMoon() {}

    public static boolean isActive(ClientLevel level) {
        if (level == null) {
            return false;
        }
        long time = level.getDayTime();
        int phase = level.getMoonPhase() & 7;
        long recent = time - phase * TICKS_PER_DAY;                 // прошедшее полнолуние
        long next = time + (8 - phase) * TICKS_PER_DAY;              // будущее полнолуние
        // на новолунии (phase 4) оба полнолуния в четырёх днях — проверяем оба
        return (phase <= WINDOW && isFirstSummerFull(recent))
                || (8 - phase <= WINDOW && isFirstSummerFull(next));
    }

    /** Это полнолуние — первое летнее: само летом, а предыдущее полнолуние было ещё не летом. */
    private static boolean isFirstSummerFull(long full) {
        return Seasons.of(full) == Calendar.Season.SUMMER
                && Seasons.of(full - LUNATION) != Calendar.Season.SUMMER;
    }
}

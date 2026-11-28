package io.github.ChanesCMD.trailcraft.client.nightsky;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import net.minecraft.client.multiplayer.ClientLevel;

import java.time.LocalDate;

/**
 * Редкая арбузная луна включается в двух случаях:
 * 1) реальное 3 августа — Всемирный день арбуза (весь реальный день);
 * 2) первое полнолуние игрового года, оканчивающегося на 1: 621, 631, 641, 651… (раз в 10 игровых лет),
 *    и ±4 игровых дня вокруг него.
 * В дни вокруг полнолуния видны все фазы: 4, 5, 6, 7 — до, полная (кот) — в полнолуние, 1, 2, 3, 4 — после.
 */
public final class WatermelonMoon {
    private static final long TICKS_PER_DAY = 24_000L;
    /** Полнолуния повторяются каждые 8 игровых суток. */
    private static final long LUNATION = 8L * TICKS_PER_DAY;
    /** За сколько дней до/после полнолуния показывать арбузные фазы. */
    private static final int WINDOW = 4;

    private WatermelonMoon() {}

    public static boolean isActive(ClientLevel level) {
        if (level == null) {
            return false;
        }
        // Всемирный день арбуза — реальное 3 августа
        LocalDate today = RealMoon.today();
        if (today.getMonthValue() == 8 && today.getDayOfMonth() == 3) {
            return true;
        }
        long time = level.getDayTime();
        int phase = level.getMoonPhase() & 7;
        long recent = time - phase * TICKS_PER_DAY;                 // прошедшее полнолуние
        long next = time + (8 - phase) * TICKS_PER_DAY;              // будущее полнолуние
        // на новолунии (phase 4) оба полнолуния в четырёх днях — проверяем оба
        return (phase <= WINDOW && isFirstFullOfYear(recent))
                || (8 - phase <= WINDOW && isFirstFullOfYear(next));
    }

    /** Полнолуние — первое в игровом году, номер которого оканчивается на 1. */
    private static boolean isFirstFullOfYear(long full) {
        int days = ServerConfig.daysPerSeason();
        Calendar cal = Calendar.of(full, days);
        long year = cal.year();
        // 621, 631, 641, 651… — раз в 10 игровых лет
        if (year % 10 != 1) {
            return false;
        }
        // предыдущее полнолуние должно быть в прошлом году — значит, это первое в этом
        return Calendar.of(full - LUNATION, days).year() != year;
    }
}

package io.github.ChanesCMD.trailcraft.calendar;

/**
 * Замедляет смену дня и ночи. Ванильная игра каждый тик прибавляет к времени суток 1 (сутки = 20 минут);
 * мы откатываем этот шаг и прибавляем дробную долю, чтобы сутки длились заданное число минут.
 * Если время прыгнуло не на 1 (сон, /time set, пакет с сервера), шаг не трогаем.
 */
public class DayTimeStepper {
    private long last = Long.MIN_VALUE;
    private double carry;

    /** @return новое время суток, которое нужно выставить, или -1, если менять ничего не надо. */
    public long step(long current, int dayLengthMinutes) {
        if (dayLengthMinutes <= 0 || current != last + 1) {
            last = current;
            return -1;
        }
        carry += (double) Calendar.TICKS_PER_DAY / (dayLengthMinutes * 60 * 20);
        long add = (long) carry;
        carry -= add;
        last += add;
        return last;
    }
}

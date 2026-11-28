package io.github.ChanesCMD.trailcraft.client.nightsky;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Реальное полнолуние по системной дате — как рождество и хэллоуин в ванили, только проверяется настоящий
 * календарный день, а не игровые сутки. Фаза Луны считается по средним элементам (Meeus, усечённый ряд):
 * угол Солнце—Луна—Земля в местный полдень. Если он меньше ~7°, значит, полнолуние приходится на этот
 * день (в полдень до/после точки полнолуния не больше 12 часов). Точность ряда — минуты, для даты хватает.
 */
public final class RealMoon {
    private static final double J2000 = 2451545.0;
    /** Юлианская дата начала Unix-эпохи. */
    private static final double UNIX_EPOCH_JD = 2440587.5;
    /** cos(7°): полдень реального дня полнолуния попадает в этот допуск. */
    private static final double FULL_MOON_COS = Math.cos(Math.toRadians(7.0));

    private static LocalDate cachedDate;
    private static boolean cachedFullMoon;

    private RealMoon() {}

    /** Сегодня (по системной дате) в реальной жизни полнолуние. */
    public static boolean isFullMoonDay() {
        LocalDate today = today();
        if (!today.equals(cachedDate)) {
            cachedDate = today;
            cachedFullMoon = fullMoonOn(today);
        }
        return cachedFullMoon;
    }

    /** Системная дата. */
    static LocalDate today() {
        return LocalDate.now();
    }

    private static boolean fullMoonOn(LocalDate date) {
        ZonedDateTime noon = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault());
        double d = julianDay(noon.toInstant()) - J2000;
        double elongation = Math.toRadians(297.8501921 + 12.19074912 * d);
        double sunAnomaly = Math.toRadians(357.5291092 + 0.98560028 * d);
        double moonAnomaly = Math.toRadians(134.9633964 + 13.06499295 * d);
        double phaseAngle = 180.0 - Math.toDegrees(elongation)
                - 6.289 * Math.sin(moonAnomaly)
                + 2.100 * Math.sin(sunAnomaly)
                - 1.274 * Math.sin(2.0 * elongation - moonAnomaly)
                - 0.658 * Math.sin(2.0 * elongation)
                - 0.214 * Math.sin(2.0 * moonAnomaly)
                - 0.110 * Math.sin(elongation);
        return Math.cos(Math.toRadians(Math.IEEEremainder(phaseAngle, 360.0))) > FULL_MOON_COS;
    }

    private static double julianDay(Instant instant) {
        return UNIX_EPOCH_JD + instant.toEpochMilli() / 86_400_000.0;
    }
}

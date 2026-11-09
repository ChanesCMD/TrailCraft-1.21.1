package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Плавный фактор ночи: 0 днём, 1 около полуночи; длина ночи зависит от сезона, яркость — от луны. */
public final class NightDarkness {
    public static float factor(Level level) {
        if (level == null) {
            return 0f;
        }
        long time = Math.floorMod(level.getDayTime(), 24000L);
        Calendar.Season season = Seasons.current();
        int start = switch (season) {
            case SUMMER -> 12500;
            case WINTER -> 11500;
            default -> 12000;
        };
        int end = switch (season) {
            case SUMMER -> 22500;
            case WINTER -> 23500;
            default -> 23000;
        };
        float base;
        if (time < start || time >= end) {
            base = 0f;
        } else if (time < start + 1000) {
            base = smooth((time - start) / 1000f);
        } else if (time > end - 1000) {
            base = smooth((end - time) / 1000f);
        } else {
            base = 1f;
        }
        // полная луна светлее, новолуние — самое тёмное
        float moon = 1f - 0.35f * level.getMoonBrightness();
        return base * moon;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private NightDarkness() {}
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Тёмная ночь как в моде True Darkness: гасится сам небесный свет, а не накладывается затемнение на экран.
 * Процент (команда /trailcraft nightDarkness 0..100) задаёт, насколько глубоко ночью пропадает свет неба:
 * 0 — ванильная ночь, 100 — неосвещённые места полностью чёрные.
 */
public final class TrueDarkness {
    /** Затемнение в полнолуние и новолуние (доля, а не проценты). */
    private static final float FULL_MOON = 0.80f, NEW_MOON = 0.88f;

    /** Насколько сейчас глубокая ночь: 0 днём, 1 около полуночи; длина ночи зависит от сезона, глубина — от луны. */
    public static float night(Level level) {
        if (level == null) {
            return 0f;
        }
        long time = Math.floorMod(level.getDayTime(), 24000L);
        int start = switch (Seasons.current()) {
            case SUMMER -> 12500;
            case WINTER -> 11500;
            default -> 12000;
        };
        int end = switch (Seasons.current()) {
            case SUMMER -> 22500;
            case WINTER -> 23500;
            default -> 23000;
        };
        float base;
        if (time < start || time >= end) {
            base = 0f;
        } else if (time < start + 500) {
            base = smooth((time - start) / 500f);
        } else if (time > end - 500) {
            base = smooth((end - time) / 500f);
        } else {
            base = 1f;
        }
        // луна не спасает: ночь в True Darkness тёмная при любой фазе
        return base;
    }

    /** Полнолуние светлее (80 %), новолуние темнее (88 %), между ними — плавно по фазе луны. */
    public static float moonDarkening(ClientLevel level) {
        float moon = Mth.clamp(level.getMoonBrightness(), 0f, 1f);   // 1 — полнолуние, 0 — новолуние
        return Mth.lerp(moon, NEW_MOON, FULL_MOON);
    }

    /** 0…1: сколько света неба отнять сейчас (процент из команды, фаза луны и глубина ночи). */
    public static float strength(ClientLevel level) {
        float moon = ClientConfig.moonPhases() ? moonDarkening(level) : 1f;
        return Mth.clamp(ClientConfig.nightDarkness() * moon, 0f, 1f) * night(level);
    }

    /**
     * Значение, которое подставляется вместо ванильного skyDarken в лайтмап.
     * В игре свет неба умножается на f * 0.95 + 0.05 — этот остаток 5 % и не даёт ночи стать чёрной.
     * При 100 % уводим f в минус так, чтобы ровно этот множитель стал нулём: свет неба пропадает,
     * а свет факелов и лавы считается отдельно и остаётся прежним.
     */
    public static float skyDarken(ClientLevel level, float vanilla) {
        float strength = strength(level);
        return vanilla - (vanilla * 0.95f + 0.05f) / 0.95f * strength;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private TrueDarkness() {}
}

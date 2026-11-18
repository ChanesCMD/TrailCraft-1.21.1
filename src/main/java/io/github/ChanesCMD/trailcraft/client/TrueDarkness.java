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
    /** Окна ночи: темнеет уже после захода солнца, светает к восходу. */
    private static final int RAMP = 2000;
    /** За тик сила меняется не больше чем на столько: после /time set тьма наезжает плавно. */
    private static final float STEP = 0.006f;
    private static float current;

    /** Насколько сейчас глубокая ночь: 0 днём, 1 в полночь; окна зависят от сезона. */
    public static float night(Level level) {
        if (level == null) {
            return 0f;
        }
        long time = Math.floorMod(level.getDayTime(), 24000L);
        // солнце садится в 12000 и полностью уходит около 13000; восход — 23000
        int start = switch (Seasons.current()) {
            case SUMMER -> 13500;
            case WINTER -> 12500;
            default -> 13000;
        };
        int end = switch (Seasons.current()) {
            case SUMMER -> 21900;
            case WINTER -> 23600;
            default -> 22900;
        };
        if (time < start || time >= end) {
            return 0f;
        }
        float fadeIn = smooth((time - start) / (float) RAMP);
        float fadeOut = smooth((end - time) / (float) RAMP);
        return Math.min(fadeIn, fadeOut);
    }

    /** Тик клиента: плавно подводим текущую тьму к цели (время суток, фаза луны, процент из команды). */
    public static void tick(ClientLevel level) {
        if (level == null) {
            current = 0f;
            return;
        }
        float target = target(level);
        if (Math.abs(target - current) <= STEP) {
            current = target;
        } else {
            current += Math.signum(target - current) * STEP;
        }
    }

    /** Куда тьма должна прийти прямо сейчас. */
    private static float target(ClientLevel level) {
        float moon = ClientConfig.moonPhases() ? moonDarkening(level) : 1f;
        return Mth.clamp(ClientConfig.nightDarkness() * moon, 0f, 1f) * night(level);
    }

    /** Полнолуние светлее (80 %), новолуние темнее (88 %), между ними — плавно по фазе луны. */
    public static float moonDarkening(ClientLevel level) {
        float moon = Mth.clamp(level.getMoonBrightness(), 0f, 1f);   // 1 — полнолуние, 0 — новолуние
        return Mth.lerp(moon, NEW_MOON, FULL_MOON);
    }

    /** 0…1: сколько света неба отнять сейчас (уже сглажено по тикам). */
    public static float strength(ClientLevel level) {
        return current;
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

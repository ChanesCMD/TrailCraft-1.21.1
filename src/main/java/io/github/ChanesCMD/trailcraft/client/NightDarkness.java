package io.github.ChanesCMD.trailcraft.client;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Плавный фактор ночи: 0 днём, 1 около полуночи. */
public final class NightDarkness {
    public static float factor(Level level) {
        if (level == null) {
            return 0f;
        }
        long time = Math.floorMod(level.getDayTime(), 24000L);
        if (time < 12000 || time >= 24000) {
            return 0f;
        }
        if (time < 14000) {
            return smooth((time - 12000f) / 2000f);
        }
        if (time < 22000) {
            return 1f;
        }
        return smooth((24000f - time) / 2000f);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private NightDarkness() {}
}

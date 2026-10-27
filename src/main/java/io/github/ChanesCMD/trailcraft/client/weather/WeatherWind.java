package io.github.ChanesCMD.trailcraft.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ветер для погодных частиц. Считается только из времени мира, поэтому у всех игроков на сервере дует одинаково:
 * направление медленно поворачивается (сумма синусов с несоизмеримыми периодами, полный круг — за несколько
 * игровых дней), сила растёт с дождём и грозой, поверх — порывы в несколько секунд.
 */
public final class WeatherWind {
    /** Сила ветра без дождя, прибавка в дождь, в грозу и от порывов (0..1). */
    static final float CALM = 0.12f, RAIN = 0.3f, THUNDER = 0.45f, GUST = 0.2f;

    /** Сила ветра от 0 (штиль) до 1 (буря). */
    public static float strength(ClientLevel level, float partialTick) {
        double t = level.getGameTime() + partialTick;
        float gust = (float) Math.max(0, Math.sin(t * 0.021) * Math.sin(t * 0.0067 + 1.3));
        return Mth.clamp(CALM + RAIN * level.getRainLevel(partialTick) + THUNDER * level.getThunderLevel(partialTick)
                + GUST * gust, 0f, 1f);
    }

    /** Направление ветра — угол в радианах (0 — на восток, +X). */
    public static double angle(ClientLevel level, float partialTick) {
        double t = level.getGameTime() + partialTick;
        return Math.sin(t * 0.00031) * 2.3 + Math.sin(t * 0.000113 + 1.7) * 1.9 + t * 0.00002;
    }

    /** Ветер по горизонтали: единичное направление, умноженное на силу (y = 0). */
    public static Vec3 vector(ClientLevel level, float partialTick) {
        double a = angle(level, partialTick);
        float s = strength(level, partialTick);
        return new Vec3(Math.cos(a) * s, 0, Math.sin(a) * s);
    }

    private WeatherWind() {}
}

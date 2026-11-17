package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Сезонный оттенок неба и тумана, как в TFC: тёплый летом, янтарный осенью, холодный зимой. */
public final class SeasonSky {
    public static Vec3 sky(Vec3 color, Level level) {
        float[] tint = tint(level);
        double red = color.x * tint[0];
        double green = color.y * tint[1];
        double blue = color.z * tint[2];
        // ночью небо ровно чёрное: никакого градиента и оттенков (иначе на тёмном фоне видны ступеньки)
        float night = NightDarkness.factor(level);
        if (night > 0f) {
            double k = 1.0 - night;
            red *= k;
            green *= k;
            blue *= k;
        }
        return new Vec3(red, green, blue);
    }

    public static float[] tint(Level level) {
        Calendar.Season season = Seasons.current();
        return switch (season) {
            case SPRING -> new float[]{0.96f, 1.0f, 0.97f};
            case SUMMER -> new float[]{1.0f, 0.94f, 0.78f};
            case AUTUMN -> new float[]{1.0f, 0.80f, 0.62f};
            case WINTER -> new float[]{0.78f, 0.88f, 1.0f};
        };
    }

    private SeasonSky() {}
}

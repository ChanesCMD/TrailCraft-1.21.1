package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.collapse.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Сезоны по календарю верхнего мира. Летом всё как было; осенью листва желтеет, трава слегка желтеет, дождь чаще;
 * зимой холоднее на {@link #WINTER_COLD} (идёт снег везде, где есть осадки, вода замерзает), листва белая, трава
 * побелевшая; весной трава зеленее, снег и лёд тают. Тёплые биомы ({@link #isWarm}: жёлтозём и его рощи, саванна,
 * эвкалиптовый, эбеновый леса, пальмовый берег, пустыни) весной и осенью как летом, зимой без снега и холода — только
 * трава и листва чуть желтеют.
 * Сезон — общий для сервера и клиента (в одиночной игре они считают его из одного dayTime): сервер обновляет его
 * в {@link SeasonEvents}, клиент — в client/SeasonColors. Температуру биома сдвигает mixin/BiomeMixin.
 */
public final class Seasons {
    /** На сколько зимой холоднее (кроме тёплых биомов): самый тёплый из остальных (0,75) — ниже порога снега 0,15. */
    public static final float WINTER_COLD = 1.0f;
    /** Биомы с базовой температурой от этой — тёплые (полоса жёлтозёма — 0,8 и выше, чернозём — до 0,75). */
    public static final float WARM_BIOME = 0.8f;
    /** Осенью перерыв между дождями — 2/3 обычного. */
    public static final float AUTUMN_RAIN_DELAY = 2f / 3f;

    private static volatile Calendar.Season current = Calendar.Season.SUMMER;

    private Seasons() {}

    public static Calendar.Season current() {
        return current;
    }

    public static Calendar.Season of(long dayTime) {
        return Calendar.of(dayTime, ServerConfig.daysPerSeason()).season();
    }

    public static void update(long dayTime) {
        current = of(dayTime);
    }

    public static boolean isWarm(Biome biome) {
        return biome.getBaseTemperature() >= WARM_BIOME;
    }

    /** Сдвиг температуры биома в текущий сезон: зимой холоднее, кроме тёплых биомов. */
    public static float temperatureOffset(Biome biome) {
        return current == Calendar.Season.WINTER && !isWarm(biome) ? -WINTER_COLD : 0f;
    }

    /**
     * Тают ли здесь снег и лёд: не зима (в тёплых биомах — всегда), биом тёплый для дождя (горные снега остаются),
     * не поставлено игроком.
     */
    public static boolean melts(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        return (current != Calendar.Season.WINTER || isWarm(biome)) && biome.warmEnoughToRain(pos)
                && !PlacedBlocks.isPlaced(level, pos);
    }
}

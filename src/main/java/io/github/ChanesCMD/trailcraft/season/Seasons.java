package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.collapse.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Сезоны по календарю верхнего мира. Летом всё как было; осенью листва желтеет, трава слегка желтеет, дождь чаще;
 * зимой холоднее на {@link #WINTER_COLD} (идёт снег везде, где есть осадки, вода замерзает), листва белая, трава
 * побелевшая; весной трава зеленее, снег и лёд тают.
 * Сезон — общий для сервера и клиента (в одиночной игре они считают его из одного dayTime): сервер обновляет его
 * в {@link SeasonEvents}, клиент — в client/SeasonColors. Температуру биома сдвигает mixin/BiomeMixin.
 */
public final class Seasons {
    /** На сколько зимой холоднее: самый тёплый биом с осадками (1,1) получает 0,1 — ниже порога снега 0,15. */
    public static final float WINTER_COLD = 1.0f;
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

    /** Сдвиг температуры биома в текущий сезон. */
    public static float temperatureOffset() {
        return current == Calendar.Season.WINTER ? -WINTER_COLD : 0f;
    }

    /** Тают ли здесь снег и лёд: не зима, биом тёплый для дождя (горные снега остаются), не поставлено игроком. */
    public static boolean melts(Level level, BlockPos pos) {
        return current != Calendar.Season.WINTER && level.getBiome(pos).value().warmEnoughToRain(pos)
                && !PlacedBlocks.isPlaced(level, pos);
    }
}

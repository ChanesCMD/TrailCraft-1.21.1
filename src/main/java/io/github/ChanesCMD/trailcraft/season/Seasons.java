package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.collapse.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Сезоны по календарю верхнего мира. Летом всё как было; осенью листва желтеет, трава слегка желтеет, дождь чаще;
 * зимой холоднее на {@link #WINTER_COLD} (идёт снег везде, где есть осадки, вода замерзает), листва белая, трава
 * побелевшая; весной трава зеленее, снег и лёд тают. Тёплые биомы ({@link #isWarm}: жёлтозём и его рощи, саванна,
 * эвкалиптовый, эбеновый леса, пальмовый берег, пустыни) весной и осенью как летом, зимой без снега и холода — только
 * трава и листва чуть желтеют.
 * Переход плавный: последние {@link #transitionDays} дней сезона (3 из 12, 6 из 24) он перетекает в следующий —
 * температура меняется непрерывно ({@link #progress}), цвета — раз в день ({@link #stage}, client/SeasonColors).
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
    /** Сколько в среднем игровых суток тает слой снега или блок льда, когда стало тепло. */
    public static final float MELT_DAYS = 1f;

    private static volatile Calendar.Season current = Calendar.Season.SUMMER;
    /** Доля перехода в следующий сезон, непрерывно: 0 — вне перехода, к концу сезона — почти 1. */
    private static volatile float progress;
    /** День перехода: 0 — вне перехода, 1..transitionDays. */
    private static volatile int stage;

    private Seasons() {}

    public static Calendar.Season current() {
        return current;
    }

    public static Calendar.Season next() {
        return Calendar.Season.values()[(current.ordinal() + 1) % 4];
    }

    public static float progress() {
        return progress;
    }

    public static int stage() {
        return stage;
    }

    /** Дней перехода в конце сезона: четверть сезона (3 при 12 днях, 6 при 24). */
    public static int transitionDays() {
        return ServerConfig.daysPerSeason() / 4;
    }

    /** Доля перехода по дням (для цветов): в n-й день перехода — n / (дней + 1), вне перехода — 0. */
    public static float stageProgress() {
        int s = stage;
        return s == 0 ? 0f : s / (float) (transitionDays() + 1);
    }

    public static Calendar.Season of(long dayTime) {
        return Calendar.of(dayTime, ServerConfig.daysPerSeason()).season();
    }

    public static void update(long dayTime) {
        int days = ServerConfig.daysPerSeason();
        Calendar cal = Calendar.of(dayTime, days);
        long t = Math.max(0, dayTime + Calendar.MIDNIGHT_OFFSET);
        double day = cal.dayOfSeason() - 1 + (t % Calendar.TICKS_PER_DAY) / (double) Calendar.TICKS_PER_DAY;
        int transition = transitionDays();
        double into = day - (days - transition);
        current = cal.season();
        progress = into <= 0 ? 0f : (float) Math.min(1, into / transition);
        stage = into < 0 ? 0 : (int) into + 1;
    }

    public static boolean isWarm(Biome biome) {
        return biome.getBaseTemperature() >= WARM_BIOME;
    }

    private static float cold(Calendar.Season season, Biome biome) {
        return season == Calendar.Season.WINTER && !isWarm(biome) ? -WINTER_COLD : 0f;
    }

    /** Сдвиг температуры биома: зимой холоднее (кроме тёплых биомов), в переходе — плавно между сезонами. */
    public static float temperatureOffset(Biome biome) {
        float from = cold(current, biome), to = cold(next(), biome), p = progress;
        return from + (to - from) * p;
    }

    /**
     * Тают ли здесь снег и лёд: здесь тепло для дождя (зимой — нет; горные снега остаются, весь год холодно),
     * не поставлено игроком.
     */
    public static boolean melts(Level level, BlockPos pos) {
        return level.getBiome(pos).value().warmEnoughToRain(pos) && !PlacedBlocks.isPlaced(level, pos);
    }

    /** Шанс растаять за случайный тик, чтобы в среднем таяло за {@link #MELT_DAYS} суток при любой длине суток. */
    public static float meltChance(ServerLevel level) {
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        int minutes = ServerConfig.dayLengthMinutes();
        float dayTicks = minutes > 0 ? minutes * 1200f : Calendar.TICKS_PER_DAY;
        return speed <= 0 ? 1f : Math.min(1f, 4096f / speed / (MELT_DAYS * dayTicks));
    }
}

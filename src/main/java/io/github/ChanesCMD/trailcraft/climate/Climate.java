package io.github.ChanesCMD.trailcraft.climate;

import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import io.github.ChanesCMD.trailcraft.block.TrailCampfireBlock;
import io.github.ChanesCMD.trailcraft.smelting.BloomeryBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Температура воздуха в °C: от биома (его базовой температуры) и сезона, с плавным переходом между сезонами
 * (Seasons.progress), суточным ходом, высотой, пещерами и дождём. Огонь рядом греет ({@link #heat}).
 * Тёплые биомы (Seasons.isWarm) за год меняются слабо, остальные — сильно. Примеры (лето / зима, средняя за сутки):
 * бурозём 20 / −10, тайга 16 / −14, чернозём 24 / −6, жёлтозём 28 / 16, саванна 30 / 18, снежные пики −4 / −34.
 */
public final class Climate {
    /** Летом: SUMMER_BASE + SUMMER_PER_TEMP × температура биома, не выше SUMMER_MAX. */
    public static final float SUMMER_BASE = 10f, SUMMER_PER_TEMP = 20f, SUMMER_MAX = 42f;
    /** Насколько весна/осень и зима холоднее лета: обычные биомы, тёплые. */
    public static final float MID_COLDER = 10f, WINTER_COLDER = 30f, WARM_MID_COLDER = 3f, WARM_WINTER_COLDER = 12f;
    /** Суточный ход: ± градусов, холоднее всего в 03:00, теплее всего в 15:00. */
    public static final float DAY_SWING = 4f;
    /** Выше этой высоты холоднее на ALTITUDE_COOLING градусов за блок (6° на 100 блоков). */
    public static final int ALTITUDE_START = 80;
    public static final float ALTITUDE_COOLING = 0.06f;
    /** В пещере (без неба) температура тянется к CAVE на долю CAVE_PULL. */
    public static final float CAVE = 12f, CAVE_PULL = 0.7f;
    /** В дождь и снег холоднее. */
    public static final float RAIN_COOLING = 3f;
    /** Огонь рядом: радиус поиска, тепло вплотную у горящего блока (костёр, печь, огонь), у лавы, предел. */
    public static final int HEAT_RADIUS = 4;
    public static final float FIRE_HEAT = 18f, LAVA_HEAT = 22f, MAX_HEAT = 30f;

    private Climate() {}

    /** Средняя за сутки температура биома в сезон. */
    public static float seasonal(Biome biome, Calendar.Season season) {
        float summer = Math.min(SUMMER_MAX, SUMMER_BASE + SUMMER_PER_TEMP * biome.getBaseTemperature());
        boolean warm = Seasons.isWarm(biome);
        return switch (season) {
            case SUMMER -> summer;
            case SPRING, AUTUMN -> summer - (warm ? WARM_MID_COLDER : MID_COLDER);
            case WINTER -> summer - (warm ? WARM_WINTER_COLDER : WINTER_COLDER);
        };
    }

    /** Средняя за сутки температура в этом месте (для растений): биом, сезон с переходом, высота, пещера. */
    public static float mean(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        float t = Mth.lerp(Seasons.progress(), seasonal(biome, Seasons.current()), seasonal(biome, Seasons.next()));
        if (pos.getY() > ALTITUDE_START) {
            t -= (pos.getY() - ALTITUDE_START) * ALTITUDE_COOLING;
        }
        if (underground(level, pos)) {
            t += (CAVE - t) * CAVE_PULL;
        }
        return t;
    }

    /** Температура воздуха сейчас: средняя, суточный ход, дождь (без огня рядом). */
    public static float air(Level level, BlockPos pos) {
        float t = mean(level, pos);
        boolean cave = underground(level, pos);
        float hour = (level.getDayTime() + Calendar.MIDNIGHT_OFFSET) % Calendar.TICKS_PER_DAY / 1000f;
        float swing = -Mth.cos((hour - 3f) / 24f * Mth.TWO_PI) * DAY_SWING;
        t += cave ? swing * (1 - CAVE_PULL) : swing;
        if (!cave && level.isRaining() && level.getBiome(pos).value().hasPrecipitation()) {
            t -= RAIN_COOLING;
        }
        return t;
    }

    /** Нет неба — пещера или глубоко под землёй. */
    private static boolean underground(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos) == 0;
    }

    /** Тепло от огня рядом: горящие костры и печи (в том числе малая сыродутная), огонь, лава; убывает с расстоянием. */
    public static float heat(Level level, BlockPos center) {
        float sum = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -HEAT_RADIUS; dx <= HEAT_RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -HEAT_RADIUS; dz <= HEAT_RADIUS; dz++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    float source = source(level.getBlockState(p));
                    if (source > 0) {
                        float d = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                        sum += source * Math.max(0, 1 - Math.max(0, d - 1) / HEAT_RADIUS);
                    }
                }
            }
        }
        return Math.min(MAX_HEAT, sum);
    }

    private static float source(BlockState state) {
        if (state.getBlock() instanceof BaseFireBlock) {
            return FIRE_HEAT;
        }
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return LAVA_HEAT;
        }
        Block block = state.getBlock();
        boolean stove = block instanceof CampfireBlock || block instanceof AbstractFurnaceBlock
                || block instanceof TrailCampfireBlock || block instanceof BloomeryBlock;
        return stove && state.getValue(BlockStateProperties.LIT) ? FIRE_HEAT : 0;
    }
}

package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.season.Seasons;
import io.github.ChanesCMD.trailcraft.season.SweptSnow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Снег в клетке с мелким блоком (трава, камешки, кремень, ветки, кусты, дикий джут). Такой блок имеет свойство SNOW
 * (0 — снега нет, 1–8 слоёв, как у снега). Снегопад поднимает SNOW вместо того, чтобы заменить блок снегом
 * (mixin/ServerLevelMixin → {@link #fall}), блок торчит из снега, снег тает по слою ({@link #melt}, из randomTick блока),
 * метла его сметает (BroomItem). Вид — в blockstate: слой снега и (у растений) побелевшая модель; у двойных растений
 * снег только в нижней половине.
 */
public final class SnowCover {
    public static final IntegerProperty SNOW = IntegerProperty.create("snow", 0, 8);

    /** Может ли снегопад класть снег в этот блок. */
    public static boolean accepts(BlockState state) {
        if (!state.hasProperty(SNOW) || !state.getFluidState().isEmpty()) {
            return false;
        }
        return !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    /** Лежит ли в блоке снег. */
    public static boolean has(BlockState state) {
        return state.hasProperty(SNOW) && state.getValue(SNOW) > 0;
    }

    /** Снегопад: слой снега в блок, если здесь холодно, не сметено и не слишком светло. */
    public static void fall(ServerLevel level, Biome biome, BlockPos pos, BlockState state) {
        if (biome.getPrecipitationAt(pos) != Biome.Precipitation.SNOW || SweptSnow.isSwept(level, pos)
                || level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
            return;
        }
        int limit = Math.min(8, level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT));
        int layers = state.getValue(SNOW);
        if (layers < limit) {
            set(level, pos, state, layers + 1);
        }
    }

    /**
     * Задаёт число слоёв снега. У двойного растения снег пишется в обе половины: нижняя рисует слой снега, а обе
     * половины — побелевшую модель; считается и тает снег по нижней половине.
     */
    public static void set(ServerLevel level, BlockPos pos, BlockState state, int layers) {
        level.setBlockAndUpdate(pos, state.setValue(SNOW, layers));
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            BlockPos up = pos.above();
            BlockState upper = level.getBlockState(up);
            if (upper.is(state.getBlock()) && upper.hasProperty(SNOW)) {
                level.setBlockAndUpdate(up, upper.setValue(SNOW, layers));
            }
        }
    }

    /** Таяние по слою, как у снега (mixin/SnowLayerBlockMixin). true — блок изменился, остальное в этот тик делать не нужно. */
    public static boolean melt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int layers = has(state) ? state.getValue(SNOW) : 0;
        if (layers > 0 && (level.getBrightness(LightLayer.BLOCK, pos) > 11
                || random.nextFloat() < Seasons.meltChance(level) && Seasons.melts(level, pos))) {
            set(level, pos, state, layers - 1);
            return true;
        }
        return false;
    }

    private SnowCover() {}
}

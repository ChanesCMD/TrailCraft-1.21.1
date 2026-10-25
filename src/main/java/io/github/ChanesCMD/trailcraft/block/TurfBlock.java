package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Дёрн на почве (жёлтозём, бурозём, чернозём, суглинок, глины, торф). Ведёт себя как ванильный блок травы:
 * если сверху блок, через который не проходит свет (или вода), дёрн отмирает и остаётся голая почва того же вида;
 * при свете от 9 сам зарастает соседние блоки почвы своим дёрном (каждая почва — своим) и ванильную землю — травой.
 */
public class TurfBlock extends Block {
    private static final List<TurfBlock> ALL = new ArrayList<>();
    private static Map<Block, Block> turfBySoil;

    private final Supplier<? extends Block> soil;

    public TurfBlock(Properties properties, Supplier<? extends Block> soil) {
        super(properties);
        this.soil = soil;
        ALL.add(this);
    }

    /** Дёрн для блока почвы или null, если это не почва. */
    private static Block turfFor(Block block) {
        if (turfBySoil == null) {
            Map<Block, Block> map = new HashMap<>();
            for (TurfBlock turf : ALL) {
                map.put(turf.soil.get(), turf);
            }
            map.put(Blocks.DIRT, Blocks.GRASS_BLOCK);
            turfBySoil = map;
        }
        return turfBySoil.get(block);
    }

    /** Может ли на этом месте жить дёрн: сверху тонкий снег или блок, пропускающий свет, и не полный блок воды. */
    private static boolean canBeTurf(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (aboveState.getFluidState().getAmount() == 8) {
            return false;
        }
        int light = LightEngine.getLightBlockInto(level, state, pos, aboveState, above, Direction.UP, aboveState.getLightBlock(level, above));
        return light < level.getMaxLightLevel();
    }

    private static boolean canSpreadTo(BlockState state, LevelReader level, BlockPos pos) {
        return canBeTurf(state, level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canBeTurf(state, level, pos)) {
            if (level.isAreaLoaded(pos, 1)) {
                level.setBlockAndUpdate(pos, soil.get().defaultBlockState());
            }
            return;
        }
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            Block turf = turfFor(level.getBlockState(target).getBlock());
            if (turf != null && canSpreadTo(turf.defaultBlockState(), level, target)) {
                level.setBlockAndUpdate(target, turf.defaultBlockState());
            }
        }
    }
}

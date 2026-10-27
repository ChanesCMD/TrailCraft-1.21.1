package io.github.ChanesCMD.trailcraft.worldgen;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Упавшая ветка на земле (только на суше). Нужна там, где нет деревьев, чтобы добыть палки. */
public class FallenBranchFeature extends Feature<NoneFeatureConfiguration> {
    public FallenBranchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos pos = ctx.origin();
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!level.getBlockState(pos).isAir() || !ground.isFaceSturdy(level, below, Direction.UP)
                || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS) || ground.is(ModTags.THIN_LOG_BLOCKS)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.FALLEN_BRANCH.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
}

package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Свой сахарный тростник: растёт, как ванильный, у воды, но на наших почвах и песке (#trailcraft:cane_soils).
 * Из него сахар, на прессе Create — бумага.
 */
public class TrailSugarCaneBlock extends SugarCaneBlock {
    public static final MapCodec<SugarCaneBlock> CODEC = simpleCodec(TrailSugarCaneBlock::new);

    public TrailSugarCaneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SugarCaneBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(this)) {
            return true;
        }
        if (!below.is(ModTags.CANE_SOILS)) {
            return false;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.below().relative(dir);
            if (level.getFluidState(side).is(FluidTags.WATER) || level.getBlockState(side).is(Blocks.FROSTED_ICE)) {
                return true;
            }
        }
        return false;
    }
}

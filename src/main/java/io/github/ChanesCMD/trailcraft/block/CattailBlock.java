package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.water.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рогоз: два блока, растёт из пресной воды. Нижняя половина стоит над водой (снизу — вода),
 * поэтому в солёной воде (Water.isSalty) не выживает.
 */
public class CattailBlock extends DoublePlantBlock {
    public CattailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level instanceof Level lvl && Water.isSalty(lvl, pos)) {
            return false;
        }
        return super.canSurvive(state, level, pos);
    }
}

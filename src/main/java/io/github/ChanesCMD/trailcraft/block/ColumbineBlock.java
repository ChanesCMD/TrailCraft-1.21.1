package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Водосбор: обычная трава, но растёт только на глине (любого цвета и её дёрне). */
public class ColumbineBlock extends GrassPlantBlock {
    public ColumbineBlock(Properties properties, int height) {
        super(properties, height);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.CLAY_WHITE.get()) || state.is(ModBlocks.CLAY_BLUE.get())
                || state.is(ModBlocks.CLAY_RED.get()) || state.is(ModBlocks.CLAY_GRAY.get())
                || state.is(ModBlocks.CLAY_WHITE_GRASS.get()) || state.is(ModBlocks.CLAY_BLUE_GRASS.get())
                || state.is(ModBlocks.CLAY_RED_GRASS.get()) || state.is(ModBlocks.CLAY_GRAY_GRASS.get());
    }
}

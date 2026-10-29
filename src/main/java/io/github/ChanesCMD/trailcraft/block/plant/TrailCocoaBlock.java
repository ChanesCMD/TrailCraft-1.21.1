package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Своё какао: стручки на стволах пальмы и эбенового дерева (#trailcraft:cocoa_logs), растут, как ванильные. Ставятся
 * своими какао-бобами; в мире — CocoaPodFeature.
 */
public class TrailCocoaBlock extends CocoaBlock {
    public static final MapCodec<CocoaBlock> CODEC = simpleCodec(TrailCocoaBlock::new);

    public TrailCocoaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CocoaBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModTags.COCOA_LOGS);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.COCOA_BEANS.get());
    }
}

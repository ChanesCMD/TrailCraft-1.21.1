package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Бревно или древесина, которые топор превращает в обтёсанный вариант. С окорки падает кора (из неё бумага), с хвойных
 * (#trailcraft:resin_logs) ещё и смола с шансом {@link #RESIN_CHANCE}.
 */
public class StrippableLogBlock extends RotatedPillarBlock {
    public static final float RESIN_CHANCE = 0.25f;
    private final Supplier<? extends Block> stripped;

    public StrippableLogBlock(Supplier<? extends Block> stripped, BlockBehaviour.Properties properties) {
        super(properties);
        this.stripped = stripped;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP) {
            if (!simulate && !context.getLevel().isClientSide) {
                var level = context.getLevel();
                var pos = context.getClickedPos();
                Block.popResourceFromFace(level, pos, context.getClickedFace(), new ItemStack(ModItems.BARK.get()));
                if (state.is(ModTags.RESIN_LOGS) && level.random.nextFloat() < RESIN_CHANCE) {
                    Block.popResourceFromFace(level, pos, context.getClickedFace(), new ItemStack(ModItems.RESIN.get()));
                }
            }
            return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}

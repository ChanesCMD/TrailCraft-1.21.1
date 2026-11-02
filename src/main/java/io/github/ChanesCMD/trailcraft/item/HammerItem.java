package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.AnvilRock;
import io.github.ChanesCMD.trailcraft.block.StoneAnvilBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import io.github.ChanesCMD.trailcraft.ore.Rock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Каменный молоток: твёрдый камешек на обработанной палке. Нужен в крафте шихты бронз (дробить куски руды) —
 * остаётся в сетке и теряет 1 прочности за крафт.
 */
public class HammerItem extends Item {
    public static final int DURABILITY = 64;

    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Rock source = state.is(ModTags.ROCKS) ? Rock.of(state.getBlock()) : null;
        AnvilRock anvilRock = source == null ? null : AnvilRock.of(source);
        if (anvilRock == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, ModBlocks.STONE_ANVIL.get().defaultBlockState().setValue(StoneAnvilBlock.ROCK, anvilRock), 3);
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f);
            if (context.getPlayer() != null) {
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack rest = stack.copy();
        rest.setDamageValue(rest.getDamageValue() + 1);
        return rest.getDamageValue() >= rest.getMaxDamage() ? ItemStack.EMPTY : rest;
    }
}

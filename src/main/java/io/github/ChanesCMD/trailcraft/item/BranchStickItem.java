package io.github.ChanesCMD.trailcraft.item;

import java.util.List;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Обычная ветка. Если ПКМ по породе 3 раза, из одной ветки получается обработанная палка.
 * Счётчик хранится на отдельной ветке: при ПКМ одна ветка из стака получает +1 и переносится в другой слот,
 * остальные ветки в руке не меняются. Ветки с одинаковым счётчиком складываются вместе.
 */
public class BranchStickItem extends Item {
    public static final int RUBS_NEEDED = 3;

    public BranchStickItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ModTags.ROCKS)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack held = context.getItemInHand();
            Player player = context.getPlayer();
            int rubs = held.getOrDefault(ModComponents.RUBS.get(), 0) + 1;
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
            if (rubs >= RUBS_NEEDED) {
                held.shrink(1);
                give(player, level, pos, new ItemStack(ModItems.STICK_PROCESSED_02.get()));
            } else if (held.getCount() == 1) {
                held.set(ModComponents.RUBS.get(), rubs);
            } else {
                // Обрабатывается только одна ветка: она отделяется от стака и уходит в другой слот,
                // а оставшиеся в руке ветки свой счётчик не меняют.
                ItemStack one = held.split(1);
                one.set(ModComponents.RUBS.get(), rubs);
                give(player, level, pos, one);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, Level level, BlockPos pos, ItemStack stack) {
        if (player == null || !player.getInventory().add(stack)) {
            Block.popResource(level, pos.above(), stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int rubs = stack.getOrDefault(ModComponents.RUBS.get(), 0);
        if (rubs > 0) {
            tooltip.add(Component.translatable("tooltip.trailcraft.rubs", rubs, RUBS_NEEDED).withStyle(ChatFormatting.GRAY));
        }
    }
}

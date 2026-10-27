package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Кувшин (как малый сосуд в TerraFirmaCraft): ПКМ в правой руке — окно на 4 слота, туда кладут куски руды, шихту и
 * слитки (Smelting.melts). Полный кувшин плавят в малой сыродутной печи. Содержимое — в компоненте trailcraft:contents.
 */
public class JugItem extends Item {
    public static final int SLOTS = 4, SLOT_LIMIT = 16;

    public JugItem(Properties properties) {
        super(properties);
    }

    /** Содержимое кувшина (всегда SLOTS стаков, пустые — EMPTY). */
    public static NonNullList<ItemStack> contents(ItemStack jug) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = jug.get(ModComponents.CONTENTS.get());
        if (contents != null) {
            contents.copyInto(items);
        }
        return items;
    }

    public static void clear(ItemStack jug) {
        jug.remove(ModComponents.CONTENTS.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new JugMenu(id, inventory), stack.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        NonNullList<ItemStack> items = contents(stack);
        int units = Smelting.units(items);
        if (units == 0) {
            tooltip.add(Component.translatable("tooltip.trailcraft.jug.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                tooltip.add(Component.literal(item.getCount() + " × ").append(item.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
        for (ItemStack ingot : Smelting.result(items)) {
            tooltip.add(Component.translatable("tooltip.trailcraft.jug.result", ingot.getCount(), ingot.getHoverName())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}

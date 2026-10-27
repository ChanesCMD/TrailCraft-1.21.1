package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

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
}

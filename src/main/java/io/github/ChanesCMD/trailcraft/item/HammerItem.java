package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

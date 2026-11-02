package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/** Зубило: в крафте «зубило + камушек → каменный кирпич» остаётся и теряет прочность. */
public class ChiselItem extends TieredToolItem {
    public ChiselItem(Tier tier, Properties properties) {
        super(tier, properties);
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

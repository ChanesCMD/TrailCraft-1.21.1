package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Пила (как в TerraFirmaCraft): бревно с пилой в сетке крафта — доски; без пилы досок, а значит и верстака, нет.
 * Пила остаётся в сетке и теряет 1 прочности за крафт, как каменный молоток.
 */
public class SawItem extends TieredToolItem {
    public SawItem(Tier tier, Item.Properties properties) {
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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.saw").withStyle(ChatFormatting.GRAY));
    }
}

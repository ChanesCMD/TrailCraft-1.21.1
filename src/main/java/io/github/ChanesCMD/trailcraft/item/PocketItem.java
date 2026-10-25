package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Карман рюкзака. Кладётся в слот кармана и открывает слоты основного инвентаря (сколько — см. BackpackItem). */
public class PocketItem extends Item {
    private final int slots;

    public PocketItem(Item.Properties properties, int slots) {
        super(properties);
        this.slots = slots;
    }

    /** Сколько слотов основного инвентаря открывает карман. */
    public int slots() {
        return slots;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.pocket.slots", slots).withStyle(ChatFormatting.GRAY));
    }
}

package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Тёплая одежда для слотов «Тепло» (inventory/PlayerSlots.WARMTH): шапка, куртка, штаны, обувь — каждая в свой слот.
 * Греет на {@link #warmth} градусов (climate/ClimateEvents): одетый полностью не мёрзнет до −20 °C.
 */
public class WarmClothingItem extends Item {
    /** Номер слота «Тепло»: 0 — шапка, 1 — куртка, 2 — штаны, 3 — обувь. */
    private final int part;
    private final int warmth;

    public WarmClothingItem(Properties properties, int part, int warmth) {
        super(properties.stacksTo(1));
        this.part = part;
        this.warmth = warmth;
    }

    public int part() {
        return part;
    }

    public int warmth() {
        return warmth;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.warm_clothing", warmth).withStyle(ChatFormatting.GRAY));
    }
}

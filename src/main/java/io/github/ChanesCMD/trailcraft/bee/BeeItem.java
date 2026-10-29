package io.github.ChanesCMD.trailcraft.bee;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Пчела одного вида: живёт в пасеке (ApiaryBlockEntity); в подсказке — откуда она и какие соты даёт. */
public class BeeItem extends Item {
    public final BeeSpecies species;

    public BeeItem(BeeSpecies species, Properties properties) {
        super(properties.stacksTo(16));
        this.species = species;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((species.wild()
                ? Component.translatable("tooltip.trailcraft.bee.wild." + species.id)
                : Component.translatable("tooltip.trailcraft.bee.bred",
                        Component.translatable("item.trailcraft." + species.parentA.id + "_bee"),
                        Component.translatable("item.trailcraft." + species.parentB.id + "_bee"))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.trailcraft.bee.comb", Component.translatable("item.trailcraft." + species.comb))
                .withStyle(ChatFormatting.GRAY));
    }
}

package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/** Кошелёк: носится в слоте кошелька, монеты лежат в нём самом (компонент CONTENTS, см. WalletHandler). */
public class WalletItem extends Item {
    public WalletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(ModComponents.CONTENTS.get());
        if (contents == null) {
            return;
        }
        for (ItemStack coin : contents.nonEmptyItems()) {
            tooltip.add(Component.literal(coin.getCount() + " × ").append(coin.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}

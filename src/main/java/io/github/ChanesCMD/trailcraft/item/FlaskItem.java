package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.water.Water;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Фляга с питьевой водой. Носится на поясе; пьют кнопкой «Выпить» в окне пояса. Наполняется сама, когда игрок
 * в воде или пьёт воду ПКМ (см. WaterEvents). Запас — в глотках, полоска под предметом показывает остаток.
 */
public class FlaskItem extends Item {
    public FlaskItem(Item.Properties properties) {
        super(properties);
    }

    public static int water(ItemStack stack) {
        return stack.getOrDefault(ModComponents.WATER.get(), 0);
    }

    public static void setWater(ItemStack stack, int water) {
        stack.set(ModComponents.WATER.get(), Mth.clamp(water, 0, Water.FLASK_SIPS));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * water(stack) / Water.FLASK_SIPS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3F76E4;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.flask.water", water(stack), Water.FLASK_SIPS).withStyle(ChatFormatting.GRAY));
    }
}

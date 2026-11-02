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
 * Фляга с водой. Носится на поясе; пьют кнопкой «Выпить», выливают кнопкой «Вылить» в окне пояса. Наполняется сама,
 * когда игрок в воде или пьёт воду ПКМ (см. WaterEvents): пресной или морской (компонент salty), но не смешивая —
 * воду другого вида нальёт только пустая фляга. Запас — в глотках, полоска под предметом показывает остаток.
 */
public class FlaskItem extends Item {
    private final int capacity;

    public FlaskItem(Item.Properties properties) {
        this(properties, Water.FLASK_SIPS);
    }

    public FlaskItem(Item.Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof FlaskItem flask ? flask.capacity : Water.FLASK_SIPS;
    }

    public static int water(ItemStack stack) {
        return stack.getOrDefault(ModComponents.WATER.get(), 0);
    }

    public static void setWater(ItemStack stack, int water) {
        stack.set(ModComponents.WATER.get(), Mth.clamp(water, 0, capacity(stack)));
        if (water <= 0) {
            stack.remove(ModComponents.SALTY.get());
        }
    }

    public static boolean isSalty(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SALTY.get(), false);
    }

    /** Наполнить флягу доверху водой этого вида. */
    public static void fill(ItemStack stack, boolean salty) {
        setWater(stack, capacity(stack));
        if (salty) {
            stack.set(ModComponents.SALTY.get(), true);
        } else {
            stack.remove(ModComponents.SALTY.get());
        }
    }

    /** Можно долить воду этого вида: фляга не полная и пустая либо с водой того же вида. */
    public static boolean canFill(ItemStack stack, boolean salty) {
        int water = water(stack);
        return water < capacity(stack) && (water == 0 || isSalty(stack) == salty);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * water(stack) / capacity(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isSalty(stack) ? 0x5E8CE8 : 0x4E81E6;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.flask.water", water(stack), capacity(stack)).withStyle(ChatFormatting.GRAY));
        if (water(stack) > 0) {
            tooltip.add(Component.translatable(isSalty(stack) ? "tooltip.trailcraft.flask.sea" : "tooltip.trailcraft.flask.fresh")
                    .withStyle(isSalty(stack) ? ChatFormatting.AQUA : ChatFormatting.BLUE));
        }
    }
}

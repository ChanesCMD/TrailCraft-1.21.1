package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Кувшин (как малый сосуд в TerraFirmaCraft): ПКМ в правой руке — окно на 4 слота, туда кладут куски руды, шихту и
 * слитки (Smelting.melts, по 16) и мелочь — палки, солому, кирпичи, комки глины… (тэг JUG_ITEMS, по 64).
 * В малой сыродутной печи кувшин нагревается (компонент trailcraft:heat, 0..MAX_HEAT): с MELT_HEAT металл в нём
 * становится жидким (Molten, компонент trailcraft:molten) и его можно разлить по формам, пока кувшин не остыл.
 * На воле кувшин остывает сам; содержимое — в компоненте trailcraft:contents.
 */
public class JugItem extends Item {
    public static final int SLOTS = 4, SLOT_LIMIT = 16, SMALL_LIMIT = 64;
    /** Нагрев: полный, с которого металл жидкий и льётся, и как часто печь греет (тиков на 1 %) и кувшин остывает. */
    public static final int MAX_HEAT = 100, MELT_HEAT = 80;
    public static final int HEAT_EVERY = 10, COOL_EVERY = 20;
    public static final TagKey<Item> JUG_ITEMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "jug_items"));

    public JugItem(Properties properties) {
        super(properties);
    }

    /** Можно ли положить предмет в кувшин. */
    public static boolean fits(ItemStack stack) {
        return Smelting.melts(stack) || stack.is(JUG_ITEMS);
    }

    /** Сколько таких предметов влезает в слот кувшина. */
    public static int limit(ItemStack stack) {
        return Smelting.melts(stack) ? SLOT_LIMIT : SMALL_LIMIT;
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

    /** Жидкий металл в кувшине или null. */
    public static Molten molten(ItemStack jug) {
        return jug.get(ModComponents.MOLTEN.get());
    }

    /** Записывает остаток жидкого металла (0 — кувшин пуст). */
    public static void setMolten(ItemStack jug, Molten molten) {
        if (molten == null || molten.amount() <= 0) {
            jug.remove(ModComponents.MOLTEN.get());
        } else {
            jug.set(ModComponents.MOLTEN.get(), molten);
        }
    }

    /** Нагрев кувшина в процентах, 0 — холодный. */
    public static int heat(ItemStack jug) {
        Integer heat = jug.get(ModComponents.HEAT.get());
        return heat == null ? 0 : heat;
    }

    public static void setHeat(ItemStack jug, int heat) {
        heat = Mth.clamp(heat, 0, MAX_HEAT);
        if (heat <= 0) {
            jug.remove(ModComponents.HEAT.get());
        } else {
            jug.set(ModComponents.HEAT.get(), heat);
        }
    }

    /** Жидкость достаточно горячая, чтобы литься в форму. */
    public static boolean canPour(ItemStack jug) {
        return heat(jug) >= MELT_HEAT;
    }

    /** Без печи кувшин остывает сам: 1 % в секунду. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        int heat = heat(stack);
        if (heat <= 0 || level.isClientSide || level.getGameTime() % COOL_EVERY != 0) {
            return;
        }
        setHeat(stack, heat - 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Molten molten = molten(stack);
        if (molten != null) {
            tooltip.add(molten.describe().withStyle(ChatFormatting.GOLD));
        }
        int heat = heat(stack);
        if (heat > 0) {
            tooltip.add(Component.translatable("tooltip.trailcraft.jug.heat", heat)
                    .withStyle(canPour(stack) ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            if (molten != null && !canPour(stack)) {
                tooltip.add(Component.translatable("tooltip.trailcraft.jug.cold", MELT_HEAT)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
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

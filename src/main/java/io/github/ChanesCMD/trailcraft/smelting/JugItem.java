package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Кувшин (как малый сосуд в TerraFirmaCraft): ПКМ в правой руке — окно на 4 слота, туда кладут куски руды, шихту и
 * слитки (Smelting.melts, по 16) и мелочь — палки, солому, кирпичи, комки глины… (тэг JUG_ITEMS, по 64). Кувшин только
 * с металлами плавят в малой сыродутной печи — в нём остаётся жидкий металл (Molten, компонент trailcraft:molten),
 * его разливают по формам в окне кувшина. Содержимое — в компоненте trailcraft:contents.
 */
public class JugItem extends Item {
    public static final int SLOTS = 4, SLOT_LIMIT = 16, SMALL_LIMIT = 64;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Molten molten = molten(stack);
        if (molten != null) {
            tooltip.add(molten.describe().withStyle(ChatFormatting.GOLD));
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

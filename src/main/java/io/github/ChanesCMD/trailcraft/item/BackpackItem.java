package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Рюкзак. Носится в слоте рюкзака или в левой руке и открывает слоты основного инвентаря.
 * Всё содержимое (основной инвентарь, служебные слоты, карманы) хранится в компоненте CONTENTS самого рюкзака.
 * В хотбар не кладётся. Если выбросить — ставится на землю блоком (см. BackpackBlock).
 */
public class BackpackItem extends BlockItem {
    // === Разметка содержимого ===
    public static final int MAIN = 0;            // 54: основной инвентарь 9×6
    public static final int MAIN_SIZE = 54;
    public static final int BATTERY = 54, SOLAR = 55, MANA = 56, SLEEPING = 57, WORKBENCH = 58, ATLAS = 59, CALENDAR = 60;
    public static final int POCKETS = 61;        // 6
    public static final int SIZE = 67;

    /** Обычный карман добавляет 3 слота, большой — 6. */
    public static final int SMALL_POCKET_SLOTS = 3, LARGE_POCKET_SLOTS = 6;

    /** Что можно положить в служебные слоты (по порядку: аккумулятор … календарь). */
    private static final List<TagKey<Item>> UTILITY = List.of(tag("batteries"), tag("solar_panels"), tag("mana_tablets"),
            tag("sleeping_bags"), tag("workbenches"), tag("atlases"), tag("calendars"));
    public static final TagKey<Item> SMALL_POCKETS = tag("small_pockets");
    public static final TagKey<Item> LARGE_POCKETS = tag("large_pockets");

    /** Сколько слотов основного инвентаря открыто без карманов. TODO: уточнить тиры рюкзаков. */
    private final int baseSlots;

    public BackpackItem(Block block, Item.Properties properties, int baseSlots) {
        super(block, properties);
        this.baseSlots = baseSlots;
    }

    /** Рюкзак в левой руке носится, а не ставится: иначе любой ПКМ пустой правой рукой ставил бы его на землю. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getHand() == InteractionHand.OFF_HAND ? InteractionResult.PASS : super.useOn(context);
    }

    /** Число открытых слотов основного инвентаря: база тира + карманы. */
    public int capacity(List<ItemStack> contents) {
        int slots = baseSlots;
        for (int i = POCKETS; i < SIZE; i++) {
            ItemStack pocket = contents.get(i);
            if (pocket.is(SMALL_POCKETS)) slots += SMALL_POCKET_SLOTS;
            else if (pocket.is(LARGE_POCKETS)) slots += LARGE_POCKET_SLOTS;
        }
        return Math.min(slots, MAIN_SIZE);
    }

    /** Подходит ли предмет в ячейку содержимого. Рюкзак в рюкзак и любые предметы с содержимым класть нельзя. */
    public static boolean accepts(int slot, ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem || stack.has(ModComponents.CONTENTS.get())) {
            return false;
        }
        if (slot < MAIN_SIZE) return true;
        if (slot < POCKETS) return stack.is(UTILITY.get(slot - BATTERY));
        return stack.is(SMALL_POCKETS) || stack.is(LARGE_POCKETS);
    }

    /** Надевает рюкзак: в свой слот, иначе в левую руку. В хотбар и основной инвентарь рюкзак не кладётся. */
    public static boolean equip(Player player, ItemStack stack) {
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        if (slots.getStackInSlot(PlayerSlots.BACKPACK).isEmpty() && slots.isItemValid(PlayerSlots.BACKPACK, stack)) {
            slots.setStackInSlot(PlayerSlots.BACKPACK, stack);
            return true;
        }
        if (player.getOffhandItem().isEmpty()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, stack);
            return true;
        }
        return false;
    }

    /** Надетый рюкзак: в своём слоте, иначе в левой руке. */
    public static ItemStack worn(Player player) {
        ItemStack back = player.getData(ModAttachments.PLAYER_SLOTS).getStackInSlot(PlayerSlots.BACKPACK);
        if (back.getItem() instanceof BackpackItem) {
            return back;
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.getItem() instanceof BackpackItem ? offhand : ItemStack.EMPTY;
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, name));
    }
}

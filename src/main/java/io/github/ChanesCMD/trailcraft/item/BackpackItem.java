package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Рюкзак. Носится в слоте рюкзака или в левой руке и открывает слоты основного инвентаря.
 * Тир рюкзака задаёт число слотов под карманы, а слоты инвентаря открывают сами карманы (PocketItem).
 * Всё содержимое (основной инвентарь, служебные слоты, карманы) хранится в компоненте CONTENTS самого рюкзака.
 * В хотбар не кладётся. Если выбросить — ставится на землю блоком (см. BackpackBlock).
 */
public class BackpackItem extends BlockItem {
    // === Разметка содержимого ===
    public static final int MAIN = 0;            // 54: основной инвентарь 9×6
    public static final int MAIN_SIZE = 54;
    public static final int BATTERY = 54, SOLAR = 55, MANA = 56, SLEEPING = 57, WORKBENCH = 58, ATLAS = 59, CALENDAR = 60;
    public static final int POCKETS = 61;        // 9: сколько из них доступно, зависит от тира
    public static final int MAX_POCKETS = 9;
    public static final int SIZE = 70;

    // === Тиры (число слотов под карманы) и карманы (сколько слотов инвентаря открывают) ===
    // Без карманов рюкзак слотов не открывает; 9 больших карманов — ровно весь инвентарь 9×6.
    public static final int DUFFEL_POCKETS = 3, LEATHER_POCKETS = 6, TACTICAL_POCKETS = 9;
    public static final int SMALL_POCKET_SLOTS = 2, MEDIUM_POCKET_SLOTS = 4, LARGE_POCKET_SLOTS = 6;

    /** Что можно положить в служебные слоты (по порядку: аккумулятор … календарь). */
    private static final List<TagKey<Item>> UTILITY = List.of(tag("batteries"), tag("solar_panels"), tag("mana_tablets"),
            tag("sleeping_bags"), tag("workbenches"), tag("atlases"), tag("calendars"));

    /** Сколько слотов под карманы у рюкзака этого тира. */
    private final int pockets;

    public BackpackItem(Block block, Item.Properties properties, int pockets) {
        super(block, properties);
        this.pockets = pockets;
    }

    public int pockets() {
        return pockets;
    }

    /** Рюкзак в левой руке носится, а не ставится: иначе любой ПКМ пустой правой рукой ставил бы его на землю. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getHand() == InteractionHand.OFF_HAND ? InteractionResult.PASS : super.useOn(context);
    }

    /** Число открытых слотов основного инвентаря: сумма по карманам. */
    public int capacity(List<ItemStack> contents) {
        int slots = 0;
        for (int i = POCKETS; i < POCKETS + pockets; i++) {
            if (contents.get(i).getItem() instanceof PocketItem pocket) slots += pocket.slots();
        }
        return Math.min(slots, MAIN_SIZE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.backpack.pockets", pockets).withStyle(ChatFormatting.GRAY));
    }

    /** Подходит ли предмет в ячейку содержимого. Рюкзак в рюкзак и любые предметы с содержимым класть нельзя. */
    public static boolean accepts(int slot, ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem || stack.has(ModComponents.CONTENTS.get())) {
            return false;
        }
        if (slot < MAIN_SIZE) return true;
        if (slot < POCKETS) return stack.is(UTILITY.get(slot - BATTERY));
        return stack.getItem() instanceof PocketItem;
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

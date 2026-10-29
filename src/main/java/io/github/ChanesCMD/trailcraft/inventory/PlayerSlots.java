package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.item.WarmClothingItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Дополнительные слоты игрока (хранятся на игроке через Data Attachment, см. ModAttachments).
 * Основной инвентарь 9×6 лежит в надетом рюкзаке (BackpackHandler), ванильные слоты 9..35 не используются.
 */
public class PlayerSlots extends ItemStackHandler {
    public static final int COSMETIC = 0;      // 4: шлем, нагрудник, поножи, ботинки
    public static final int WARMTH = 4;        // 4: шапка, куртка, штаны, обувь
    public static final int ACCESSORIES = 8;   // 8
    public static final int WALLET = 16;
    public static final int BELT = 17;
    public static final int BACKPACK = 18;
    public static final int SIZE = 19;

    public static final TagKey<Item> WARM_CLOTHING = tag("warm_clothing");
    public static final TagKey<Item> ACCESSORY_ITEMS = tag("accessories");
    public static final TagKey<Item> WALLETS = tag("wallets");
    public static final TagKey<Item> BELTS = tag("belts");
    public static final TagKey<Item> BACKPACKS = tag("backpacks");

    public PlayerSlots() {
        super(SIZE);
    }

    /** Старое сохранение с меньшим числом слотов не должно урезать список. */
    @Override
    public void setSize(int size) {
        super.setSize(Math.max(size, SIZE));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // тёплая одежда — каждая в свой слот (шапка, куртка, штаны, обувь); чужая из тэга — в любой
        if (slot >= WARMTH && slot < ACCESSORIES) return stack.getItem() instanceof WarmClothingItem w
                ? w.part() == slot - WARMTH : stack.is(WARM_CLOTHING);
        if (slot >= ACCESSORIES && slot < WALLET) return stack.is(ACCESSORY_ITEMS);
        if (slot == WALLET) return stack.is(WALLETS);
        if (slot == BELT) return stack.is(BELTS);
        if (slot == BACKPACK) return stack.is(BACKPACKS);
        return true; // косметическую броню проверяет слот меню
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, name));
    }
}

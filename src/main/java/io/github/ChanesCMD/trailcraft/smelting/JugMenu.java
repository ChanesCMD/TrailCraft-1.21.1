package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Окно кувшина: 4 слота кувшина и хотбар (слоты 9..35 в TrailCraft не используются). Сам кувшин (в выбранном слоте
 * хотбара) взять нельзя, пока окно открыто.
 */
public class JugMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, JUG_Y = 20, HOTBAR_Y = 86;
    private final Inventory inventory;

    public JugMenu(int id, Inventory inventory) {
        super(ModSmelting.JUG_MENU.get(), id);
        this.inventory = inventory;
        JugHandler jug = new JugHandler(inventory.player);
        int left = (WIDTH - JugItem.SLOTS * 18) / 2 + 1;
        for (int i = 0; i < JugItem.SLOTS; i++) {
            addSlot(new WriteBackSlot(jug, i, left + i * 18, JUG_Y, false));
        }
        for (int i = 0; i < 9; i++) {
            int slot = i;
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y) {
                @Override
                public boolean mayPickup(Player player) {
                    return slot != inventory.selected;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return slot != inventory.selected;
                }
            });
        }
    }

    /** Содержимое кувшина, как его видит окно (для подсказки «что выплавится»). */
    public java.util.List<ItemStack> jugItems() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < JugItem.SLOTS; i++) {
            items.add(slots.get(i).getItem());
        }
        return items;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // цифрой нельзя переложить сам кувшин
        if (clickType == ClickType.SWAP && button == inventory.selected) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = index < JugItem.SLOTS
                ? moveItemStackTo(stack, JugItem.SLOTS, JugItem.SLOTS + 9, false)
                : moveItemStackTo(stack, 0, JugItem.SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModSmelting.JUG.get());
    }
}

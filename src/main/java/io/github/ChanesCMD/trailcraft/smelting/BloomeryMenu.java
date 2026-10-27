package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Окно малой сыродутной печи: кувшин, топливо, 4 слота слитков и хотбар. */
public class BloomeryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, HOTBAR_Y = 86;
    public static final int JUG_X = 44, JUG_Y = 17, FUEL_X = 44, FUEL_Y = 53, OUT_X = 107, OUT_Y = 26;
    private final ContainerData data;

    /** Клиент: слоты пустые, их заполнит синхронизация. */
    public BloomeryMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(BloomeryBlockEntity.SLOTS), new SimpleContainerData(4));
    }

    public BloomeryMenu(int id, Inventory inventory, IItemHandler items, ContainerData data) {
        super(ModSmelting.BLOOMERY_MENU.get(), id);
        this.data = data;
        addSlot(new WriteBackSlot(items, BloomeryBlockEntity.JUG, JUG_X, JUG_Y, false));
        addSlot(new WriteBackSlot(items, BloomeryBlockEntity.FUEL, FUEL_X, FUEL_Y, false));
        for (int i = 0; i < 4; i++) {
            addSlot(new WriteBackSlot(items, BloomeryBlockEntity.OUT + i, OUT_X + (i % 2) * 18, OUT_Y + (i / 2) * 18, true));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    /** Огонь от 0 до 1. */
    public float burn() {
        return data.get(1) == 0 ? 0 : (float) data.get(0) / data.get(1);
    }

    /** Прогресс плавки от 0 до 1. */
    public float progress() {
        return data.get(3) == 0 ? 0 : (float) data.get(2) / data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = BloomeryBlockEntity.SLOTS;
        boolean moved = index < machine
                ? moveItemStackTo(stack, machine, machine + 9, false)
                : moveItemStackTo(stack, 0, 2, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

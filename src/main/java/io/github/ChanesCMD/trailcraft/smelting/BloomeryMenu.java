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

/**
 * Окно малой сыродутной печи: кувшин, топливо и хотбар; справа — полоска нагрева кувшина (с меткой 80 %, с которой
 * металл плавится и льётся). Выхода у печи нет, она только греет кувшин.
 */
public class BloomeryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, HOTBAR_Y = 86;
    public static final int JUG_X = 44, JUG_Y = 17, FUEL_X = 44, FUEL_Y = 53;
    /** Полоска нагрева справа от кувшина. */
    public static final int HEAT_X = 72, HEAT_Y = 26, HEAT_W = 76, HEAT_H = 8;
    private final ContainerData data;

    /** Клиент: слоты пустые, их заполнит синхронизация. */
    public BloomeryMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(BloomeryBlockEntity.SLOTS), new SimpleContainerData(3));
    }

    public BloomeryMenu(int id, Inventory inventory, IItemHandler items, ContainerData data) {
        super(ModSmelting.BLOOMERY_MENU.get(), id);
        this.data = data;
        addSlot(new WriteBackSlot(items, BloomeryBlockEntity.JUG, JUG_X, JUG_Y, false));
        addSlot(new WriteBackSlot(items, BloomeryBlockEntity.FUEL, FUEL_X, FUEL_Y, false));
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    /** Огонь от 0 до 1. */
    public float burn() {
        return data.get(1) == 0 ? 0 : (float) data.get(0) / data.get(1);
    }

    /** Кувшин в печи. */
    public ItemStack jug() {
        return slots.get(BloomeryBlockEntity.JUG).getItem();
    }

    /** Нагрев кувшина от 0 до 1. */
    public float heat() {
        return data.get(2) / (float) JugItem.MAX_HEAT;
    }

    /** Нагрет ли кувшин до температуры литья. */
    public boolean canPour() {
        return data.get(2) >= JugItem.MELT_HEAT;
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

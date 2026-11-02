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

/** Окно большого горна: руда, топливо, крица. */
public class LargeBloomeryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, HOTBAR_Y = 86;
    public static final int ORE_X = 44, ORE_Y = 17, FUEL_X = 44, FUEL_Y = 53, OUT_X = 98, OUT_Y = 35;
    private final ContainerData data;

    public LargeBloomeryMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(LargeBloomeryBlockEntity.SLOTS), new SimpleContainerData(2));
    }

    public LargeBloomeryMenu(int id, Inventory inventory, IItemHandler items, ContainerData data) {
        super(ModSmelting.LARGE_BLOOMERY_MENU.get(), id);
        this.data = data;
        addSlot(new WriteBackSlot(items, LargeBloomeryBlockEntity.ORE, ORE_X, ORE_Y, false));
        addSlot(new WriteBackSlot(items, LargeBloomeryBlockEntity.FUEL, FUEL_X, FUEL_Y, false));
        addSlot(new WriteBackSlot(items, LargeBloomeryBlockEntity.OUT, OUT_X, OUT_Y, true));
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public float progress() {
        return data.get(1) == 0 ? 0 : (float) data.get(0) / data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = LargeBloomeryBlockEntity.SLOTS;
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

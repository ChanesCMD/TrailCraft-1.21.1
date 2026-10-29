package io.github.ChanesCMD.trailcraft.bee;

import io.github.ChanesCMD.trailcraft.init.ModBees;
import io.github.ChanesCMD.trailcraft.smelting.WriteBackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Окно пасеки: две пчелы слева, стрелка цикла, 3×3 выхода справа и хотбар. */
public class ApiaryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 128, HOTBAR_Y = 102;
    public static final int BEE_X = 26, BEE_A_Y = 18, BEE_B_Y = 46, OUTPUT_X = 98, OUTPUT_Y = 14;
    private final ContainerData data;

    /** Клиент: слоты пустые, их заполнит синхронизация. */
    public ApiaryMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(ApiaryBlockEntity.SLOTS), new SimpleContainerData(3));
    }

    public ApiaryMenu(int id, Inventory inventory, IItemHandler items, ContainerData data) {
        super(ModBees.APIARY_MENU.get(), id);
        this.data = data;
        addSlot(new WriteBackSlot(items, ApiaryBlockEntity.BEE_A, BEE_X, BEE_A_Y, false));
        addSlot(new WriteBackSlot(items, ApiaryBlockEntity.BEE_B, BEE_X, BEE_B_Y, false));
        for (int i = 0; i < 9; i++) {
            addSlot(new WriteBackSlot(items, ApiaryBlockEntity.OUTPUT + i, OUTPUT_X + i % 3 * 18, OUTPUT_Y + i / 3 * 18, true));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    /** Цикл от 0 до 1. */
    public float progress() {
        return data.get(1) == 0 ? 0 : Math.min(1, (float) data.get(0) / data.get(1));
    }

    /** ApiaryBlockEntity.OK … FULL. */
    public int status() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = ApiaryBlockEntity.SLOTS;
        boolean moved = index < machine
                ? moveItemStackTo(stack, machine, machine + 9, false)
                : stack.getItem() instanceof BeeItem && moveItemStackTo(stack, 0, 2, false);
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

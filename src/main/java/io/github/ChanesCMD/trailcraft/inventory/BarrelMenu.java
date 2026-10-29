package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
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

/**
 * Окно бочки (WaterBarrelBlockEntity): слева бак (сколько и чего), слот выскобленных шкур, стрелка дубления, слот кожи;
 * снизу хотбар. Данные: количество жидкости, вид (WaterBarrelBlockEntity.EMPTY…TANNIN), тики дубления.
 */
public class BarrelMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, HOTBAR_Y = 86;
    public static final int TANK_X = 20, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    public static final int HIDES_X = 62, LEATHER_X = 116, SLOT_Y = 35;
    private final ContainerData data;

    /** Клиент: слоты пустые, их заполнит синхронизация. */
    public BarrelMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(2), new SimpleContainerData(3));
    }

    public BarrelMenu(int id, Inventory inventory, IItemHandler items, ContainerData data) {
        super(ModMenus.BARREL.get(), id);
        this.data = data;
        addSlot(new WriteBackSlot(items, WaterBarrelBlockEntity.HIDES, HIDES_X, SLOT_Y, false));
        addSlot(new WriteBackSlot(items, WaterBarrelBlockEntity.LEATHER, LEATHER_X, SLOT_Y, true));
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int amount() {
        return data.get(0);
    }

    public int fluidKind() {
        return data.get(1);
    }

    /** Дубление от 0 до 1. */
    public float soak() {
        return (float) data.get(2) / WaterBarrelBlockEntity.SOAK_TICKS;
    }

    public boolean hasHides() {
        return slots.get(WaterBarrelBlockEntity.HIDES).hasItem();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = index < 2 ? moveItemStackTo(stack, 2, 11, false) : moveItemStackTo(stack, 0, 1, false);
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

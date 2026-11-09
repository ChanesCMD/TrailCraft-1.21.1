package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
import io.github.ChanesCMD.trailcraft.smelting.WriteBackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Окно бочки в духе TFC: слева высокий бак (сколько и чего), справа слот выскобленных шкур и слот кожи,
 * между ними — вертикальный прогресс дубления, рядом кнопка запечатывания. Пока бочка запечатана, слоты
 * заперты, а дубление идёт; открытая бочка хранит прогресс. Снизу хотбар.
 * Данные: количество жидкости, вид (WaterBarrelBlockEntity.EMPTY…TANNIN), тики дубления, запечатана ли.
 */
public class BarrelMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 150, HOTBAR_Y = 124;
    public static final int TANK_X = 12, TANK_Y = 24, TANK_W = 22, TANK_H = 72;
    public static final int HIDES_X = 60, HIDES_Y = 26, LEATHER_X = 60, LEATHER_Y = 68;
    public static final int PROGRESS_X = 94, PROGRESS_Y = 30, PROGRESS_W = 7, PROGRESS_H = 62;
    public static final int SEAL_X = 122, SEAL_Y = 30, SEAL_SIZE = 22;
    private static final int CONTAINER_SLOTS = 2;

    private final ContainerData data;
    @Nullable
    private final WaterBarrelBlockEntity barrel;

    /** Клиент: слоты пустые, их заполнит синхронизация. */
    public BarrelMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(2), new SimpleContainerData(4), null);
    }

    /** Сервер: окно живёт на самой бочке, поэтому кнопка запечатывания меняет её состояние. */
    public BarrelMenu(int id, Inventory inventory, WaterBarrelBlockEntity barrel) {
        this(id, inventory, barrel.items, barrel.data, barrel);
    }

    private BarrelMenu(int id, Inventory inventory, IItemHandler items, ContainerData data,
                       @Nullable WaterBarrelBlockEntity barrel) {
        super(ModMenus.BARREL.get(), id);
        this.data = data;
        this.barrel = barrel;
        addSlot(new WriteBackSlot(items, WaterBarrelBlockEntity.HIDES, HIDES_X, HIDES_Y, false));
        addSlot(new WriteBackSlot(items, WaterBarrelBlockEntity.LEATHER, LEATHER_X, LEATHER_Y, true));
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

    public boolean sealed() {
        return data.get(3) != 0;
    }

    public boolean hasHides() {
        return slots.get(WaterBarrelBlockEntity.HIDES).hasItem();
    }

    public int hides() {
        return slots.get(WaterBarrelBlockEntity.HIDES).getItem().getCount();
    }

    public int leather() {
        return slots.get(WaterBarrelBlockEntity.LEATHER).getItem().getCount();
    }

    /** Кнопка в окне: 0 — запечатать/распечатать. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && barrel != null) {
            barrel.toggleSealed();
            return true;
        }
        return false;
    }

    /** В запечатанной бочке слоты шкур и кожи не трогаются — как в TFC. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (sealed() && slotId >= 0 && slotId < CONTAINER_SLOTS) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (sealed()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = index < CONTAINER_SLOTS
                ? moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), false)
                : moveItemStackTo(stack, WaterBarrelBlockEntity.HIDES, WaterBarrelBlockEntity.HIDES + 1, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return barrel == null || player.distanceToSqr(barrel.getBlockPos().getX() + 0.5,
                barrel.getBlockPos().getY() + 0.5, barrel.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}

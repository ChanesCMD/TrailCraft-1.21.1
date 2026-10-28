package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Окно кувшина: 4 слота кувшина, слот формы и хотбар (слоты 9..35 в TrailCraft не используются). Сам кувшин (в выбранном
 * слоте хотбара) взять нельзя, пока окно открыто. Пустая форма в слоте формы сразу заливается жидким металлом из
 * кувшина (Smelting.UNIT, если металл в неё льётся); форма в слоте при закрытии окна возвращается игроку.
 */
public class JugMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 112, JUG_Y = 20, MOLD_X = 80, MOLD_Y = 56, HOTBAR_Y = 86;
    /** Номера слотов меню: 0..3 — кувшин, MOLD — форма, дальше хотбар. */
    public static final int MOLD = JugItem.SLOTS, HOTBAR = MOLD + 1;
    private final Inventory inventory;
    private final Container mold = new SimpleContainer(1);

    public JugMenu(int id, Inventory inventory) {
        super(ModSmelting.JUG_MENU.get(), id);
        this.inventory = inventory;
        JugHandler jug = new JugHandler(inventory.player);
        int left = (WIDTH - JugItem.SLOTS * 18) / 2 + 1;
        for (int i = 0; i < JugItem.SLOTS; i++) {
            addSlot(new WriteBackSlot(jug, i, left + i * 18, JUG_Y, false));
        }
        addSlot(new Slot(mold, 0, MOLD_X, MOLD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MoldItem;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                pour();
            }
        });
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

    /** Сервер: пустая форма в слоте — залить её металлом из кувшина. */
    private void pour() {
        Player player = inventory.player;
        ItemStack form = mold.getItem(0);
        ItemStack jug = player.getMainHandItem();
        if (player.level().isClientSide || !(form.getItem() instanceof MoldItem item) || MoldItem.cast(form) != null
                || !jug.is(ModSmelting.JUG.get())) {
            return;
        }
        Molten molten = JugItem.molten(jug);
        if (molten == null || molten.amount() < Smelting.UNIT || item.mold.result(molten.metal()).isEmpty()) {
            return;
        }
        ItemStack filled = form.copy();
        filled.set(ModComponents.CAST.get(), molten.metal());
        JugItem.setMolten(jug, new Molten(molten.metal(), molten.amount() - Smelting.UNIT));
        mold.setItem(0, filled);
        player.level().playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    /** Содержимое кувшина, как его видит окно (для подсказки «что выплавится»). */
    public java.util.List<ItemStack> jugItems() {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < JugItem.SLOTS; i++) {
            items.add(slots.get(i).getItem());
        }
        return items;
    }

    /** Жидкий металл в кувшине (на клиенте — из синхронизированного предмета в руке) или null. */
    public Molten molten() {
        return JugItem.molten(inventory.player.getMainHandItem());
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
        boolean moved;
        if (index < HOTBAR) {
            moved = moveItemStackTo(stack, HOTBAR, HOTBAR + 9, false);
        } else if (stack.getItem() instanceof MoldItem) {
            moved = moveItemStackTo(stack, MOLD, MOLD + 1, false);
        } else {
            moved = moveItemStackTo(stack, 0, JugItem.SLOTS, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, mold);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModSmelting.JUG.get());
    }
}

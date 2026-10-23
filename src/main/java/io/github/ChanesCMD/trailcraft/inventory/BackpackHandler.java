package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.Collections;

/**
 * Слоты надетого рюкзака. Содержимое читается из компонента рюкзака и при каждом изменении записывается обратно,
 * поэтому если рюкзак сняли или переложили, слоты сразу показывают новое состояние.
 * Без рюкзака все слоты пустые и ничего не принимают.
 */
public class BackpackHandler implements IItemHandlerModifiable {
    private final Player player;
    private final NonNullList<ItemStack> items = NonNullList.withSize(BackpackItem.SIZE, ItemStack.EMPTY);
    private ItemStack owner = ItemStack.EMPTY;
    private ItemContainerContents loaded;

    public BackpackHandler(Player player) {
        this.player = player;
    }

    /** Живой список содержимого; перечитывается, если рюкзак сменился или его содержимое изменили снаружи. */
    private NonNullList<ItemStack> items() {
        ItemStack stack = BackpackItem.worn(player);
        ItemContainerContents contents = stack.isEmpty() ? null : stack.get(ModComponents.CONTENTS.get());
        if (stack != owner || contents != loaded) {
            owner = stack;
            loaded = contents;
            Collections.fill(items, ItemStack.EMPTY);
            if (contents != null) {
                contents.copyInto(items);
            }
        }
        return items;
    }

    public boolean isPresent() {
        items();
        return !owner.isEmpty();
    }

    /** Сколько слотов основного инвентаря открыто. */
    public int capacity() {
        NonNullList<ItemStack> list = items();
        return owner.getItem() instanceof BackpackItem backpack ? backpack.capacity(list) : 0;
    }

    public boolean has(int slot) {
        return isPresent() && !items.get(slot).isEmpty();
    }

    private void save() {
        if (!owner.isEmpty()) {
            loaded = ItemContainerContents.fromItems(items);
            owner.set(ModComponents.CONTENTS.get(), loaded);
        }
    }

    @Override
    public int getSlots() {
        return BackpackItem.SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items().get(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        items().set(slot, stack);
        save();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isPresent() && (slot >= BackpackItem.MAIN_SIZE || slot < capacity()) && BackpackItem.accepts(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot < BackpackItem.MAIN_SIZE ? 99 : 1;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack existing = getStackInSlot(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                setStackInSlot(slot, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
                setStackInSlot(slot, existing);
            }
        }
        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = getStackInSlot(slot);
        if (amount <= 0 || existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(amount, existing.getCount());
        if (!simulate) {
            setStackInSlot(slot, existing.getCount() > count ? existing.copyWithCount(existing.getCount() - count) : ItemStack.EMPTY);
        }
        return existing.copyWithCount(count);
    }

    /** Кладёт предмет в открытые слоты основного инвентаря. @return остаток */
    public ItemStack insertMain(ItemStack stack, boolean simulate) {
        int capacity = capacity();
        for (int i = 0; i < capacity && !stack.isEmpty(); i++) {
            if (!getStackInSlot(i).isEmpty()) stack = insertItem(i, stack, simulate);
        }
        for (int i = 0; i < capacity && !stack.isEmpty(); i++) {
            stack = insertItem(i, stack, simulate);
        }
        return stack;
    }
}

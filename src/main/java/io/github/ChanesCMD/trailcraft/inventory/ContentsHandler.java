package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.Collections;

/**
 * Слоты предмета-хранилища, надетого на игрока (рюкзак, кошелёк, пояс). Содержимое читается из компонента CONTENTS
 * предмета и при каждом изменении записывается обратно, поэтому если предмет сняли или переложили, слоты сразу
 * показывают новое состояние. Без предмета все слоты пустые и ничего не принимают.
 */
public abstract class ContentsHandler implements IItemHandlerModifiable {
    protected final Player player;
    private final NonNullList<ItemStack> items;
    private ItemStack owner = ItemStack.EMPTY;
    private ItemContainerContents loaded;

    protected ContentsHandler(Player player, int size) {
        this.player = player;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    /** Предмет, в котором лежит содержимое, или пустой стак. */
    protected abstract ItemStack holder();

    /** Перед записью можно переложить содержимое (кошелёк меняет монеты). */
    protected void beforeSave(NonNullList<ItemStack> items) {}

    /** Живой список содержимого; перечитывается, если предмет сменился или его содержимое изменили снаружи. */
    protected NonNullList<ItemStack> items() {
        ItemStack stack = holder();
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

    /** Надетый предмет (после перечитывания). */
    protected ItemStack owner() {
        items();
        return owner;
    }

    public boolean isPresent() {
        return !owner().isEmpty();
    }

    private void save() {
        if (!owner.isEmpty()) {
            beforeSave(items);
            loaded = ItemContainerContents.fromItems(items);
            owner.set(ModComponents.CONTENTS.get(), loaded);
        }
    }

    @Override
    public int getSlots() {
        return items.size();
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

    /** Сколько таких предметов влезает в слот (кувшин: мелочь — 64, то, что плавится, — 16). */
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack existing = getStackInSlot(slot);
        int limit = getStackLimit(slot, stack);
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
}

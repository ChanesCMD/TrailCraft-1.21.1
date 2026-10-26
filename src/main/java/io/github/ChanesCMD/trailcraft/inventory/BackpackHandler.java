package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Слоты надетого рюкзака (в слоте рюкзака или в левой руке): основной инвентарь, служебные слоты, карманы. */
public class BackpackHandler extends ContentsHandler {
    public BackpackHandler(Player player) {
        super(player, BackpackItem.SIZE);
    }

    /** Общий экземпляр игрока (сам замечает, что рюкзак сменился), чтобы не создавать новый каждый тик. */
    public static BackpackHandler of(Player player) {
        return player.getData(ModAttachments.BACKPACK_HANDLER);
    }

    @Override
    protected ItemStack holder() {
        return BackpackItem.worn(player);
    }

    /** Сколько слотов основного инвентаря открыто. */
    public int capacity() {
        return owner().getItem() instanceof BackpackItem backpack ? backpack.capacity(items()) : 0;
    }

    /** Сколько слотов под карманы у надетого рюкзака. */
    public int pockets() {
        return owner().getItem() instanceof BackpackItem backpack ? backpack.pockets() : 0;
    }

    public boolean has(int slot) {
        return isPresent() && !getStackInSlot(slot).isEmpty();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!isPresent() || slot >= BackpackItem.POCKETS + pockets()) return false;
        return (slot >= BackpackItem.MAIN_SIZE || slot < capacity()) && BackpackItem.accepts(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot < BackpackItem.MAIN_SIZE ? 99 : 1;
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

package io.github.ChanesCMD.trailcraft.smelting;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Слот хранилища, который после прямых изменений стака (grow/shrink при раскладке и Shift-клике) записывает его обратно —
 * иначе кувшин не сохранил бы содержимое в свой компонент, а печь не пометила бы себя изменённой.
 */
public class WriteBackSlot extends SlotItemHandler {
    private final boolean output;

    public WriteBackSlot(IItemHandler handler, int index, int x, int y, boolean output) {
        super(handler, index, x, y);
        this.output = output;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !output && super.mayPlace(stack);
    }

    @Override
    public void setChanged() {
        if (getItemHandler() instanceof IItemHandlerModifiable handler) {
            handler.setStackInSlot(getContainerSlot(), getItem());
        }
        super.setChanged();
    }
}

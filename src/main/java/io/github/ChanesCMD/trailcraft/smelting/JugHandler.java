package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.inventory.ContentsHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Слоты кувшина в правой руке игрока: то, что плавится, — по 16, мелочь (#trailcraft:jug_items) — по 64. */
public class JugHandler extends ContentsHandler {
    public JugHandler(Player player) {
        super(player, JugItem.SLOTS);
    }

    @Override
    protected ItemStack holder() {
        ItemStack stack = player.getMainHandItem();
        return stack.is(ModSmelting.JUG.get()) ? stack : ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // в кувшин с жидким металлом ничего не кладут, пока его не разольют
        return isPresent() && JugItem.molten(owner()) == null && JugItem.fits(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return JugItem.SMALL_LIMIT;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(JugItem.limit(stack), stack.getMaxStackSize());
    }
}

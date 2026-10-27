package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.inventory.ContentsHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Слоты кувшина в правой руке игрока. */
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
        return isPresent() && Smelting.melts(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return JugItem.SLOT_LIMIT;
    }
}

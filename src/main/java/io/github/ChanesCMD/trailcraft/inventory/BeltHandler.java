package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Содержимое надетого пояса. Пока один слот — фляга с питьевой водой (подсумки и связь — позже). */
public class BeltHandler extends ContentsHandler {
    public static final int FLASK = 0;
    public static final int SIZE = 1;

    public BeltHandler(Player player) {
        super(player, SIZE);
    }

    /** Общий экземпляр игрока (сам замечает, что пояс сменился), чтобы не создавать новый каждый тик. */
    public static BeltHandler of(Player player) {
        return player.getData(ModAttachments.BELT_HANDLER);
    }

    @Override
    protected ItemStack holder() {
        return player.getData(ModAttachments.PLAYER_SLOTS).getStackInSlot(PlayerSlots.BELT);
    }

    /** Фляга на поясе или пустой стак. */
    public ItemStack flask() {
        ItemStack stack = getStackInSlot(FLASK);
        return stack.getItem() instanceof FlaskItem ? stack : ItemStack.EMPTY;
    }

    /** Меняет запас воды во фляге на поясе. */
    public void setFlaskWater(int water) {
        ItemStack flask = flask();
        if (!flask.isEmpty()) {
            ItemStack copy = flask.copy();
            FlaskItem.setWater(copy, water);
            setStackInSlot(FLASK, copy);
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isPresent() && stack.getItem() instanceof FlaskItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
}

package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.item.CoinItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;

/**
 * Монеты в надетом кошельке: 8 ячеек, по одной на каждый вид монет (от меди до драгоценного иридия).
 * Любые положенные монеты сразу пересчитываются 10 к 1 в старшие: в младших ячейках остаётся не больше 9.
 * Старших монет в ячейке не больше стака, лишние выпадают.
 */
public class WalletHandler extends ContentsHandler {
    public WalletHandler(Player player) {
        super(player, CoinItem.TIERS);
    }

    @Override
    protected ItemStack holder() {
        return player.getData(ModAttachments.PLAYER_SLOTS).getStackInSlot(PlayerSlots.WALLET);
    }

    @Override
    protected void beforeSave(NonNullList<ItemStack> items) {
        long total = 0;
        for (ItemStack stack : items) {
            if (stack.getItem() instanceof CoinItem coin) {
                total += coin.value() * stack.getCount();
            }
        }
        Collections.fill(items, ItemStack.EMPTY);
        int top = CoinItem.TIERS - 1;
        for (int tier = 0; tier < top; tier++) {
            items.set(tier, CoinItem.stack(tier, (int) (total % CoinItem.RATE)));
            total /= CoinItem.RATE;
        }
        ItemStack highest = CoinItem.stack(top, (int) Math.min(total, Integer.MAX_VALUE));
        int max = Math.min(getSlotLimit(top), highest.getMaxStackSize());
        if (highest.getCount() > max) {
            ItemStack extra = highest.split(highest.getCount() - max);
            if (!player.level().isClientSide) {
                while (!extra.isEmpty()) {
                    player.drop(extra.split(max), false);
                }
            }
        }
        items.set(top, highest);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isPresent() && stack.getItem() instanceof CoinItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 99;
    }
}

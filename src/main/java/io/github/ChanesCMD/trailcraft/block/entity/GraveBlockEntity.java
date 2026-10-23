package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.inventory.DeathRecord;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Могила: вещи умершего игрока. Первые 2 реальных часа (DeathRecord.OWNER_ONLY_MS) открыть и сломать
 * её может только владелец, потом — кто угодно. Пустая могила остаётся стоять, пока её не сломают.
 */
public class GraveBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 54;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private UUID owner;
    private String ownerName = "";
    /** Реальное время смерти (мс); по нему могила связана с записью журнала смертей. */
    private long deathTime;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
    }

    public void setOwner(Player player, long deathTime) {
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        this.deathTime = deathTime;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** Сколько ещё могила личная (мс), 0 — открыта для всех. */
    public long privateFor() {
        return owner == null ? 0 : Math.max(0, deathTime + DeathRecord.OWNER_ONLY_MS - System.currentTimeMillis());
    }

    public boolean canAccess(Player player) {
        return isOwner(player) || privateFor() == 0;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean hasBackpack() {
        return items.stream().anyMatch(stack -> stack.getItem() instanceof BackpackItem);
    }

    /** Если владелец забрал рюкзак, отмечаем это в его журнале смертей. */
    public void checkBackpackTaken(Player player) {
        if (isOwner(player) && !hasBackpack()) {
            markBackpackTaken(player);
        }
    }

    public void markBackpackTaken(Player player) {
        DeathRecord.markTaken(player, deathTime);
    }

    /** Ч:ММ:СС */
    public static String formatDuration(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
    }

    @Override
    protected Component getDefaultName() {
        long left = privateFor();
        return left > 0
                ? Component.translatable("container.trailcraft.grave.timer", ownerName, formatDuration(left))
                : Component.translatable("container.trailcraft.grave", ownerName);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        if (level != null && !level.isClientSide) {
            checkBackpackTaken(player);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putString("owner_name", ownerName);
        tag.putLong("death_time", deathTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("owner_name");
        deathTime = tag.getLong("death_time");
    }
}

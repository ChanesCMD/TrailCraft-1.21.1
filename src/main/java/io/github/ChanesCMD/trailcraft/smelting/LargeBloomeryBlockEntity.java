package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Большой сыродутный горн: 8 кусков железной руды и 8 топлива превращаются в крицу.
 * Работает, пока цела труба 3×3×3 вокруг люка.
 */
public class LargeBloomeryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ORE = 0, FUEL = 1, OUT = 2, SLOTS = 3;
    public static final int ORE_COUNT = 8, FUEL_COUNT = 8, COOK_TICKS = 2400;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case ORE -> stack.is(ModTags.IRON_ORES);
                case FUEL -> stack.is(ModTags.LARGE_BLOOMERY_FUELS);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == OUT ? 64 : 8;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int progress, total;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : total;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else total = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public LargeBloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModSmelting.LARGE_BLOOMERY_ENTITY.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeBloomeryBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean valid = LargeBloomeryBlock.isStructureValid(level, pos, state);
        ItemStack ore = items.getStackInSlot(ORE);
        ItemStack fuel = items.getStackInSlot(FUEL);
        ItemStack out = items.getStackInSlot(OUT);
        boolean room = out.isEmpty() || out.is(ModSmelting.BLOOM.get()) && out.getCount() < out.getMaxStackSize();
        boolean work = valid && ore.getCount() >= ORE_COUNT && fuel.getCount() >= FUEL_COUNT && room;

        if (!work) {
            progress = 0;
            total = 0;
            setLit(level, pos, state, false);
            return;
        }
        if (total == 0) {
            total = COOK_TICKS;
        }
        progress++;
        if (progress >= total) {
            ore.shrink(ORE_COUNT);
            fuel.shrink(FUEL_COUNT);
            if (out.isEmpty()) {
                items.setStackInSlot(OUT, new ItemStack(ModSmelting.BLOOM.get()));
            } else {
                out.grow(1);
                items.setStackInSlot(OUT, out);
            }
            progress = 0;
            total = 0;
        }
        setLit(level, pos, state, true);
        setChanged();
    }

    private void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LargeBloomeryBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(LargeBloomeryBlock.LIT, lit), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        progress = tag.getInt("progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trailcraft.large_bloomery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LargeBloomeryMenu(id, inventory, items, data);
    }
}

package io.github.ChanesCMD.trailcraft.block.entity;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Угольная горелка: запас горения в тиках ({@link #MAX_BURN}); пока горит — нагрев «нагрето» (KINDLED) для чаши Create
 * сверху, раскалённого (SEETHING) не даёт никогда. Топливо: уголь — {@link #COAL_TICKS}, торф — вдвое меньше, блок угля
 * (#c:storage_blocks/coal) — как 9 углей. Кладут ПКМ или воронкой/желобом Create (fuelSlot).
 */
public class CoalBurnerBlockEntity extends BlockEntity {
    public static final int COAL_TICKS = 800;
    public static final int MAX_BURN = COAL_TICKS * 18;
    private int burn;

    /** Слот только для топлива: сразу сгорает в запас горения. */
    public final IItemHandler fuelSlot = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int ticks = fuelTicks(stack);
            if (ticks <= 0 || burn + ticks > MAX_BURN) {
                return stack;
            }
            if (!simulate) {
                addBurn(ticks);
            }
            ItemStack rest = stack.copy();
            rest.shrink(1);
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return fuelTicks(stack) > 0;
        }
    };

    public CoalBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_BURNER.get(), pos, state);
    }

    public static int fuelTicks(ItemStack stack) {
        if (stack.is(ModTags.COAL_BLOCKS)) {
            return COAL_TICKS * 9;
        }
        if (stack.is(ItemTags.COALS)) {
            return COAL_TICKS;
        }
        if (stack.is(ModTags.BLOOMERY_FUELS)) {
            return COAL_TICKS / 2;  // торф
        }
        return 0;
    }

    /** Подбросить топливо; false — не топливо или запас полон. */
    public boolean addFuel(ItemStack stack) {
        int ticks = fuelTicks(stack);
        if (ticks <= 0 || burn + ticks > MAX_BURN) {
            return false;
        }
        addBurn(ticks);
        return true;
    }

    private void addBurn(int ticks) {
        burn += ticks;
        setChanged();
        updateHeat();
    }

    public int burn() {
        return burn;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoalBurnerBlockEntity burner) {
        if (burner.burn > 0) {
            burner.burn--;
            if (burner.burn == 0) {
                burner.setChanged();
                burner.updateHeat();
            }
        }
    }

    private void updateHeat() {
        if (level == null) {
            return;
        }
        BlazeBurnerBlock.HeatLevel heat = burn > 0 ? BlazeBurnerBlock.HeatLevel.KINDLED : BlazeBurnerBlock.HeatLevel.NONE;
        BlockState state = getBlockState();
        if (state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != heat) {
            level.setBlock(worldPosition, state.setValue(BlazeBurnerBlock.HEAT_LEVEL, heat), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Burn", burn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burn = tag.getInt("Burn");
    }
}

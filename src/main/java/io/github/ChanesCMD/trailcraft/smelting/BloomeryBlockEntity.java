package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
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
 * Малая сыродутная печь. Слоты: 0 — кувшин, 1 — топливо (#trailcraft:bloomery_fuels: древесный и каменный уголь, торф).
 * Пока в кувшине есть что плавить (Smelting.melt), печь жжёт топливо (одно на FUEL_TICKS) и копит прогресс; готово —
 * содержимое кувшина становится жидким металлом (Molten) в нём же, кувшин остаётся в печи. Без огня прогресс понемногу
 * остывает.
 */
public class BloomeryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int JUG = 0, FUEL = 1, SLOTS = 2;
    /** Сколько тиков горит одно топливо: уголь и торф (торф — вдвое меньше). */
    public static final int FUEL_TICKS = 1200, PEAT_TICKS = 600;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case JUG -> stack.is(ModSmelting.JUG.get());
                case FUEL -> stack.is(ModTags.BLOOMERY_FUELS);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == JUG ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int burn, burnMax, progress, total;

    /** Для окна: огонь, его максимум, прогресс, сколько всего нужно. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burn;
                case 1 -> burnMax;
                case 2 -> progress;
                default -> total;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burn = value;
                case 1 -> burnMax = value;
                case 2 -> progress = value;
                default -> total = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModSmelting.BLOOMERY_ENTITY.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean wasLit = burn > 0;
        if (burn > 0) {
            burn--;
        }
        ItemStack jug = items.getStackInSlot(JUG);
        Molten result = jug.isEmpty() || JugItem.molten(jug) != null ? null : Smelting.melt(JugItem.contents(jug)).orElse(null);
        boolean work = result != null;
        if (work) {
            if (burn == 0) {
                ItemStack fuel = items.getStackInSlot(FUEL);
                if (!fuel.isEmpty()) {
                    burn = burnMax = fuel.is(ModBlocks.PEAT.get().asItem()) ? PEAT_TICKS : FUEL_TICKS;
                    items.extractItem(FUEL, 1, false);
                }
            }
            total = Smelting.time(result.amount() / Smelting.UNIT);
            if (burn > 0) {
                if (++progress >= total) {
                    ItemStack melted = jug.copy();
                    JugItem.clear(melted);
                    JugItem.setMolten(melted, result);
                    items.setStackInSlot(JUG, melted);
                    progress = 0;
                }
            } else {
                progress = Math.max(0, progress - 2);
            }
        } else {
            progress = 0;
            total = 0;
        }
        if (wasLit != burn > 0) {
            level.setBlock(pos, state.setValue(BloomeryBlock.LIT, burn > 0), 3);
        }
        if (work || wasLit) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("burn", burn);
        tag.putInt("burn_max", burnMax);
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        burn = tag.getInt("burn");
        burnMax = tag.getInt("burn_max");
        progress = tag.getInt("progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trailcraft.small_bloomery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BloomeryMenu(id, inventory, items, data);
    }
}

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
 * Выхода у печи нет: пока горит топливо, она просто греет кувшин (компонент trailcraft:heat, 1 % за HEAT_EVERY тиков
 * до MAX_HEAT). Как только нагрев дошёл до MELT_HEAT, металл в кувшине становится жидким (Molten) — дальше его льют
 * по формам в окне кувшина, пока он не остыл. Без огня кувшин в печи остывает сам.
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
    private int burn, burnMax;

    /** Для окна: огонь, его максимум и нагрев кувшина в процентах. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burn;
                case 1 -> burnMax;
                default -> JugItem.heat(items.getStackInSlot(JUG));
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burn = value;
                case 1 -> burnMax = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
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
        ItemStack jugStack = items.getStackInSlot(JUG);
        boolean changed = false;
        if (!jugStack.isEmpty()) {
            int heat = JugItem.heat(jugStack);
            // топливо нужно только пока кувшин не раскалён
            if (burn == 0 && heat < JugItem.MAX_HEAT) {
                ItemStack fuel = items.getStackInSlot(FUEL);
                if (!fuel.isEmpty()) {
                    burn = burnMax = fuel.is(ModBlocks.PEAT.get().asItem()) ? PEAT_TICKS : FUEL_TICKS;
                    items.extractItem(FUEL, 1, false);
                }
            }
            boolean burning = burn > 0;
            boolean heating = burning && heat < JugItem.MAX_HEAT && level.getGameTime() % JugItem.HEAT_EVERY == 0;
            boolean cooling = !burning && heat > 0 && level.getGameTime() % JugItem.COOL_EVERY == 0;
            if (heating || cooling) {
                JugItem.setHeat(jugStack, heat + (heating ? 1 : -1));
                changed = true;
            }
            // раскалился — металл в кувшине становится жидким
            if (JugItem.heat(jugStack) >= JugItem.MELT_HEAT && JugItem.molten(jugStack) == null) {
                Molten result = Smelting.melt(JugItem.contents(jugStack)).orElse(null);
                if (result != null) {
                    JugItem.clear(jugStack);
                    JugItem.setMolten(jugStack, result);
                    changed = true;
                }
            }
        }
        if (wasLit != burn > 0) {
            level.setBlock(pos, state.setValue(BloomeryBlock.LIT, burn > 0), 3);
        }
        if (changed || wasLit || burn > 0) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("burn", burn);
        tag.putInt("burn_max", burnMax);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        burn = tag.getInt("burn");
        burnMax = tag.getInt("burn_max");
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

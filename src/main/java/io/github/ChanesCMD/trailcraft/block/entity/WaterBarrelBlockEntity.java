package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModFluids;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import io.github.ChanesCMD.trailcraft.inventory.BarrelMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Бочка: бак на {@link #CAPACITY} mB воды или танина (трубы Create качают из неё и в неё — FluidHandler). Вода —
 * пресная или морская (salty; воду другого вида нальёт только пустая бочка), под дождём набирается сама
 * ({@link #RAIN_MB} за {@link #RAIN_INTERVAL} тиков). В танин кладут выскобленные шкуры (до {@link #MAX_HIDES}): пока в
 * бочке не меньше {@link #TANNIN_NEEDED} mB танина, они дубятся {@link #SOAK_TICKS} тиков и становятся кожей, каждая
 * тратит {@link #HIDE_MB} mB (как дубление в бочке TerraFirmaCraft). Сломанная бочка помнит жидкость (компоненты
 * trailcraft:water — mB, trailcraft:salty, trailcraft:tannin), шкуры и кожа из неё выпадают. Окно — BarrelMenu: бак, слот
 * шкур, слот кожи; воронки и желоба Create кладут шкуры и забирают кожу (automation).
 */
public class WaterBarrelBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 8000;
    /** Глоток или одно деление фляги — 100 mB. */
    public static final int SIP_MB = 100;
    public static final int RAIN_MB = 25, RAIN_INTERVAL = 40;
    public static final int MAX_HIDES = 8, TANNIN_NEEDED = 4000, HIDE_MB = 500, SOAK_TICKS = 12000;

    public static final int HIDES = 0, LEATHER = 1;
    /** Что в баке для окна: 0 — пусто, 1 — пресная вода, 2 — морская, 3 — танин. */
    public static final int EMPTY = 0, FRESH = 1, SALTY = 2, TANNIN = 3;

    private boolean salty;
    private int soak;
    /** Шкуры (только выскобленные, до MAX_HIDES) и готовая кожа. */
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == HIDES && stack.is(ModItems.SCRAPED_HIDE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == HIDES ? MAX_HIDES : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == HIDES && getStackInSlot(HIDES).isEmpty()) {
                soak = 0;
            }
            changed();
        }
    };
    /** Для воронок: шкуры только кладутся, кожа только забирается. */
    public final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == HIDES ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == LEATHER ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == HIDES && items.isItemValid(slot, stack);
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.getFluidAmount();
                case 1 -> fluidKind();
                case 2 -> soak;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 3;
        }
    };
    public final FluidTank tank = new FluidTank(CAPACITY, stack -> stack.is(Fluids.WATER) || stack.is(ModFluids.TANNIN.source.get())) {
        @Override
        protected void onContentsChanged() {
            if (isEmpty()) {
                salty = false;
            }
            changed();
        }
    };

    public WaterBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_BARREL.get(), pos, state);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Сколько воды (0, если в бочке танин). */
    public int water() {
        return tank.getFluid().is(Fluids.WATER) ? tank.getFluidAmount() : 0;
    }

    /** Сколько танина (0, если в бочке вода). */
    public int tannin() {
        return tank.getFluid().is(ModFluids.TANNIN.source.get()) ? tank.getFluidAmount() : 0;
    }

    public boolean isSalty() {
        return salty;
    }

    public int hides() {
        return items.getStackInSlot(HIDES).getCount();
    }

    public int leather() {
        return items.getStackInSlot(LEATHER).getCount();
    }

    public int fluidKind() {
        if (tannin() > 0) {
            return TANNIN;
        }
        return water() > 0 ? (salty ? SALTY : FRESH) : EMPTY;
    }

    /** Налить воду этого вида; сколько вошло. */
    public int add(int mb, boolean salty) {
        if (water() > 0 && this.salty != salty) {
            return 0;
        }
        int filled = tank.fill(new FluidStack(Fluids.WATER, mb), FluidTank.FluidAction.EXECUTE);
        if (filled > 0) {
            this.salty = salty;
        }
        return filled;
    }

    /** Налить другую жидкость (танин); сколько вошло. */
    public int add(Fluid fluid, int mb) {
        return tank.fill(new FluidStack(fluid, mb), FluidTank.FluidAction.EXECUTE);
    }

    /** Набрать жидкость; сколько вышло. */
    public int take(int mb) {
        return tank.drain(mb, FluidTank.FluidAction.EXECUTE).getAmount();
    }

    /** Положить выскобленные шкуры в танин; сколько легло. */
    public int addHides(int count) {
        ItemStack rest = items.insertItem(HIDES, new ItemStack(ModItems.SCRAPED_HIDE.get(), count), false);
        return count - rest.getCount();
    }

    /** Забрать готовую кожу. */
    public ItemStack takeLeather() {
        return items.extractItem(LEATHER, 64, false);
    }

    /** Шкуры и кожа выпадают из сломанной бочки. */
    public void dropContents() {
        if (level != null) {
            for (int i = 0; i < items.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        items.getStackInSlot(i).copy());
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterBarrelBlockEntity barrel) {
        if (level.getGameTime() % RAIN_INTERVAL == 0 && level.isRainingAt(pos.above()) && barrel.tannin() == 0) {
            barrel.add(RAIN_MB, false);
        }
        int hides = barrel.hides();
        // кожа ложится в свой слот, пока там есть место
        if (hides > 0 && barrel.tannin() >= TANNIN_NEEDED && barrel.leather() + hides <= 64 && ++barrel.soak >= SOAK_TICKS) {
            barrel.take(hides * HIDE_MB);
            barrel.items.setStackInSlot(HIDES, ItemStack.EMPTY);
            barrel.items.setStackInSlot(LEATHER, new ItemStack(ModItems.LEATHER.get(), barrel.leather() + hides));
            barrel.soak = 0;
        } else if (barrel.soak % 200 == 0 && barrel.soak > 0) {
            barrel.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
        tag.putBoolean("Salty", salty);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Soak", soak);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        salty = tag.getBoolean("Salty");
        items.deserializeNBT(registries, tag.getCompound("Items"));
        soak = tag.getInt("Soak");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelMenu(id, inventory, items, data);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (tank.getFluidAmount() > 0) {
            components.set(ModComponents.WATER.get(), tank.getFluidAmount());
            if (salty) {
                components.set(ModComponents.SALTY.get(), true);
            }
            if (tannin() > 0) {
                components.set(ModComponents.TANNIN.get(), true);
            }
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        int amount = Math.min(components.getOrDefault(ModComponents.WATER.get(), 0), CAPACITY);
        salty = components.getOrDefault(ModComponents.SALTY.get(), false);
        Fluid fluid = components.getOrDefault(ModComponents.TANNIN.get(), false) ? ModFluids.TANNIN.source.get() : Fluids.WATER;
        tank.setFluid(amount > 0 ? new FluidStack(fluid, amount) : FluidStack.EMPTY);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Fluid");
        tag.remove("Salty");
    }
}

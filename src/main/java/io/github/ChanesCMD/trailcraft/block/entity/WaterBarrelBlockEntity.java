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
import net.minecraft.world.Containers;
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
 * trailcraft:water — mB, trailcraft:salty, trailcraft:tannin), шкуры и кожа из неё выпадают.
 */
public class WaterBarrelBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8000;
    /** Глоток или одно деление фляги — 100 mB. */
    public static final int SIP_MB = 100;
    public static final int RAIN_MB = 25, RAIN_INTERVAL = 40;
    public static final int MAX_HIDES = 8, TANNIN_NEEDED = 4000, HIDE_MB = 500, SOAK_TICKS = 12000;

    private boolean salty;
    private int hides, leather, soak;
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
        return hides;
    }

    public int leather() {
        return leather;
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
        int added = Math.min(count, MAX_HIDES - hides);
        if (added > 0) {
            hides += added;
            changed();
        }
        return added;
    }

    /** Забрать готовую кожу. */
    public ItemStack takeLeather() {
        ItemStack out = new ItemStack(ModItems.LEATHER.get(), leather);
        leather = 0;
        changed();
        return out;
    }

    /** Шкуры и кожа выпадают из сломанной бочки. */
    public void dropContents() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    new ItemStack(ModItems.SCRAPED_HIDE.get(), hides));
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    new ItemStack(ModItems.LEATHER.get(), leather));
            hides = leather = 0;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterBarrelBlockEntity barrel) {
        if (level.getGameTime() % RAIN_INTERVAL == 0 && level.isRainingAt(pos.above()) && barrel.tannin() == 0) {
            barrel.add(RAIN_MB, false);
        }
        if (barrel.hides > 0 && barrel.tannin() >= TANNIN_NEEDED && ++barrel.soak >= SOAK_TICKS) {
            barrel.take(barrel.hides * HIDE_MB);
            barrel.leather += barrel.hides;
            barrel.hides = 0;
            barrel.soak = 0;
            barrel.changed();
        } else if (barrel.soak % 200 == 0 && barrel.soak > 0) {
            barrel.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
        tag.putBoolean("Salty", salty);
        tag.putInt("Hides", hides);
        tag.putInt("Leather", leather);
        tag.putInt("Soak", soak);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        salty = tag.getBoolean("Salty");
        hides = tag.getInt("Hides");
        leather = tag.getInt("Leather");
        soak = tag.getInt("Soak");
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

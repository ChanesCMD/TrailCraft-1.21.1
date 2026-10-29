package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Бочка с водой: бак на {@link #CAPACITY} mB только воды (трубы Create качают из неё и в неё — FluidHandler), вид воды
 * — пресная или морская (salty; воду другого вида нальёт только пустая бочка). Под дождём набирается сама
 * ({@link #RAIN_MB} за {@link #RAIN_INTERVAL} тиков). Сломанная бочка помнит воду (компоненты trailcraft:water — mB,
 * trailcraft:salty).
 */
public class WaterBarrelBlockEntity extends BlockEntity {
    public static final int CAPACITY = 8000;
    /** Глоток или одно деление фляги — 100 mB. */
    public static final int SIP_MB = 100;
    public static final int RAIN_MB = 25, RAIN_INTERVAL = 40;

    private boolean salty;
    public final FluidTank tank = new FluidTank(CAPACITY, stack -> stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            if (isEmpty()) {
                salty = false;
            }
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public WaterBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_BARREL.get(), pos, state);
    }

    public int water() {
        return tank.getFluidAmount();
    }

    public boolean isSalty() {
        return salty;
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

    /** Набрать воду; сколько вышло. */
    public int take(int mb) {
        return tank.drain(mb, FluidTank.FluidAction.EXECUTE).getAmount();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterBarrelBlockEntity barrel) {
        if (level.getGameTime() % RAIN_INTERVAL == 0 && level.isRainingAt(pos.above())) {
            barrel.add(RAIN_MB, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
        tag.putBoolean("Salty", salty);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        salty = tag.getBoolean("Salty");
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
        if (water() > 0) {
            components.set(ModComponents.WATER.get(), water());
            if (salty) {
                components.set(ModComponents.SALTY.get(), true);
            }
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        int water = components.getOrDefault(ModComponents.WATER.get(), 0);
        salty = components.getOrDefault(ModComponents.SALTY.get(), false);
        tank.setFluid(water > 0 ? new FluidStack(Fluids.WATER, Math.min(water, CAPACITY)) : FluidStack.EMPTY);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Fluid");
        tag.remove("Salty");
    }
}

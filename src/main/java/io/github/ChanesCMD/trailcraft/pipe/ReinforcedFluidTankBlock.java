package io.github.ChanesCMD.trailcraft.pipe;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModReinforced;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Укреплённый жидкостный бак: бак Create со своим типом блок-сущности — поэтому с медными баками Create в один
 * многоблочный бак не сливается; лава его не прожигает (LavaPipes). Котёл на нём работает, как на обычном.
 */
public class ReinforcedFluidTankBlock extends FluidTankBlock {
    public ReinforcedFluidTankBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return ModReinforced.TANK_ENTITY.get();
    }
}

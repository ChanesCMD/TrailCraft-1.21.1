package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** ВРЕМЕННАЯ: только чтобы клиент рисовал столб над самородком (см. NuggetBlock). Данных не хранит. */
public class NuggetMarkerBlockEntity extends BlockEntity {
    public NuggetMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUGGET_MARKER.get(), pos, state);
    }
}

package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Сундук своей породы: ванильный сундук со своим типом блок-сущности (WoodChestBlock). */
public class WoodChestBlockEntity extends ChestBlockEntity {
    public WoodChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_CHEST.get(), pos, state);
    }
}

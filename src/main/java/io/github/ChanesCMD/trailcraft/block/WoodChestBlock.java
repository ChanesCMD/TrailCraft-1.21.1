package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.WoodChestBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Сундук своей породы древесины (init/ModChests): ванильный сундук (двойной — из двух сундуков одной породы), текстура —
 * entity/chest/<порода>[_left|_right] (client/WoodChestRenderer).
 */
public class WoodChestBlock extends ChestBlock {
    public static final MapCodec<ChestBlock> CODEC = simpleCodec(p -> new WoodChestBlock("oak", p));
    public final String wood;

    public WoodChestBlock(String wood, Properties properties) {
        super(properties, ModBlockEntities.WOOD_CHEST::get);
        this.wood = wood;
    }

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodChestBlockEntity(pos, state);
    }
}

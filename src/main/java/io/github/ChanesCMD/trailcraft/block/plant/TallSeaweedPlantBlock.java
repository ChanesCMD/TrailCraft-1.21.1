package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Стебель длинной водоросли: растёт вверх, пока сверху вода; ломается вместе с верхушкой. */
public class TallSeaweedPlantBlock extends KelpPlantBlock {
    public static final MapCodec<TallSeaweedPlantBlock> CODEC = simpleCodec(TallSeaweedPlantBlock::new);

    public TallSeaweedPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<KelpPlantBlock> codec() {
        return (MapCodec<KelpPlantBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) ModBlocks.TALL_SEAWEED.get();
    }
}

package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Длинная водоросль (как ламинария): растёт вверх в толще воды, нижние части — {@link TallSeaweedPlantBlock}.
 * Верхний блок ломается и даёт длинную водоросль, из неё на костре получается сушёная.
 */
public class TallSeaweedBlock extends KelpBlock {
    public static final MapCodec<TallSeaweedBlock> CODEC = simpleCodec(TallSeaweedBlock::new);

    public TallSeaweedBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<KelpBlock> codec() {
        return (MapCodec<KelpBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.TALL_SEAWEED_PLANT.get();
    }
}

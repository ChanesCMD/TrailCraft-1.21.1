package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Простое растение-трава: крестообразная модель, без коллизии, растёт на дёрне и обычной земле. */
public class GrassPlantBlock extends BushBlock {
    public static final MapCodec<GrassPlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("height").forGetter(b -> b.height),
            propertiesCodec()
    ).apply(instance, (height, props) -> new GrassPlantBlock(props, height)));

    private final int height;
    private final VoxelShape shape;

    public GrassPlantBlock(BlockBehaviour.Properties properties, int height) {
        super(properties);
        this.height = height;
        this.shape = Block.box(2.0, 0.0, 2.0, 14.0, height, 14.0);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(ModTags.GRASS_PLANTABLE);
    }
}

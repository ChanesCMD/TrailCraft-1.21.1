package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Дикий джут (два блока): как обычное двойное растение, но нижнюю половину зимой заносит снегом, а белеют обе половины (SnowCover). */
public class WildJuteBlock extends DoublePlantBlock {
    public static final MapCodec<WildJuteBlock> CODEC = simpleCodec(WildJuteBlock::new);

    public WildJuteBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SnowCover.SNOW, 0));
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SnowCover.SNOW);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return SnowCover.has(state) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SnowCover.melt(state, level, pos, random);
    }
}

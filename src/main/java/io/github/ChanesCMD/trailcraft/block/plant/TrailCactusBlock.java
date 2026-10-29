package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Свой кактус пустынь: растёт, колет и стоит, как ванильный, но на нашем песке (#trailcraft:cactus_soils). На костре —
 * свой зелёный краситель.
 */
public class TrailCactusBlock extends CactusBlock {
    public static final MapCodec<CactusBlock> CODEC = simpleCodec(TrailCactusBlock::new);

    public TrailCactusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CactusBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).isSolid() || level.getFluidState(pos.relative(dir)).is(FluidTags.LAVA)) {
                return false;
            }
        }
        BlockState below = level.getBlockState(pos.below());
        return (below.is(this) || below.is(ModTags.CACTUS_SOILS)) && !level.getBlockState(pos.above()).liquid();
    }
}

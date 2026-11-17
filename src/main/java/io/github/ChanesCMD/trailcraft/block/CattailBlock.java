package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.block.SnowCover;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.water.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Рогоз: два блока в неглубокой пресной воде. Нижняя половина стоит на почве или иле под водой
 * (вода в ней сохраняется — waterlogged), верхняя — над поверхностью. В солёной воде не выживает.
 */
public class CattailBlock extends DoublePlantBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** Вода вокруг замёрзла: нижний блок рисует ещё и лёд, но сам рогоз остаётся. */
    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");

    public CattailBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(SnowCover.SNOW, 0)
                .setValue(FROZEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, SnowCover.SNOW, FROZEN);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(ModBlocks.SILT.get()) || state.is(Blocks.ICE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level instanceof Level lvl && Water.isSalty(lvl, pos)) {
            return false;
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            // Нижняя половина — в воде, над ней воздух (глубина ровно один блок).
            // Верхняя половина уже может стоять: при обновлении соседей это не повод ломать нижнюю.
            BlockState above = level.getBlockState(pos.above());
            boolean aboveOk = above.isAir() || (above.is(this) && above.getValue(HALF) == DoubleBlockHalf.UPPER);
            // вода могла замёрзнуть — тогда рогоз стоит во льду и не ломается
            boolean frozen = level.getBlockState(pos).is(Blocks.ICE) || level.getBlockState(pos.below()).is(Blocks.ICE);
            if ((!frozen && !level.getFluidState(pos).is(FluidTags.WATER))
                    || (!frozen && !level.getFluidState(pos.above()).isEmpty())
                    || !aboveOk) {
                return false;
            }
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(WATERLOGGED, context.getLevel().isWaterAt(context.getClickedPos()));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(WATERLOGGED);
    }

    /** Зимой вода в клетке рогоза замерзает и оттаивает: нижний блок показывает лёд. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        boolean freeze = level.getBiome(pos).value().shouldFreeze(level, pos, false);
        if (state.getValue(FROZEN) != freeze) {
            level.setBlockAndUpdate(pos, state.setValue(FROZEN, freeze));
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                     BlockPos pos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}

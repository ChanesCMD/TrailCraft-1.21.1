package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * Тонкое бревно 6x6 пикселей. Само соединяется с соседними тонкими и обычными брёвнами и с листвой:
 * получаются прямая (2 конца), угол, Т (3), крест и угол (4), 5 и 6 концов.
 * Если соседей нет, бревно лежит вдоль оси, по которой его поставили; если сосед один, бревно проходит насквозь.
 */
public class ThinLogBlock extends PipeBlock {
    public static final MapCodec<ThinLogBlock> CODEC = simpleCodec(ThinLogBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public ThinLogBlock(BlockBehaviour.Properties properties) {
        super(0.1875f, properties);
        BlockState state = stateDefinition.any().setValue(AXIS, Direction.Axis.Y);
        for (var prop : PROPERTY_BY_DIRECTION.values()) state = state.setValue(prop, false);
        registerDefaultState(state.setValue(UP, true).setValue(DOWN, true));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
        return withConnections(context.getLevel(), context.getClickedPos(), base);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return withConnections(level, pos, state);
    }

    private static boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof ThinLogBlock || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    /** Пересчитывает концы по соседям. */
    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        boolean[] arm = new boolean[6];
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (connectsTo(level.getBlockState(pos.relative(dir)))) {
                arm[dir.ordinal()] = true;
                count++;
            }
        }
        if (count == 0) {
            // одиночное бревно лежит вдоль оси установки
            Direction.Axis axis = state.getValue(AXIS);
            arm[Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).ordinal()] = true;
            arm[Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE).ordinal()] = true;
        } else if (count == 1) {
            // один сосед: бревно продолжается насквозь
            for (Direction dir : Direction.values()) {
                if (arm[dir.ordinal()]) {
                    arm[dir.getOpposite().ordinal()] = true;
                    break;
                }
            }
        }
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), arm[dir.ordinal()]);
        }
        return state;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

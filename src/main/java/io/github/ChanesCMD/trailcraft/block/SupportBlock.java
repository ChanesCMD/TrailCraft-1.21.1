package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Опора, как в TFC: стойка (beam = false) стоит на твёрдом блоке или на другой стойке, перекладина (beam = true)
 * висит между стойками — в каждую сторону по прямой через другие перекладины не дальше {@link #MAX_SPAN} блоков до
 * стойки — или лежит на стойке. Опора в раме ({@link #isFramed}: перекладины, по краям которых две стойки, и столбы этих
 * стоек) держит породу и почву в коробке ±horizontal по X и Z и ±vertical по Y вокруг себя (collapse/Collapse):
 * деревянная 9×5×9, железная 13×7×13; одиночная стойка или перекладина без второй стойки — ±loneHorizontal,
 * ±vertical: деревянная 3×5×3, железная 5×7×5. Ставятся одним предметом (item/SupportItem).
 * Свойства north/east/south/west — плечи к соседним перекладинам (у стойки) или к стойкам и перекладинам (у перекладины).
 */
public class SupportBlock extends Block {
    /** Самое большее перекладин подряд от стойки. */
    public static final int MAX_SPAN = 4;
    /** Самое большее свободных блоков между стойками, чтобы предмет сам поставил перекладину (SupportItem). */
    public static final int MAX_GAP = 5;
    public static final Map<Direction, BooleanProperty> ARMS = new HashMap<>();
    static {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            ARMS.put(dir, PipeBlock.PROPERTY_BY_DIRECTION.get(dir));
        }
    }
    private static final VoxelShape POST = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape CORE = Block.box(5, 10, 5, 11, 16, 11);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = Map.of(
            Direction.NORTH, Block.box(5, 10, 0, 11, 16, 5),
            Direction.SOUTH, Block.box(5, 10, 11, 11, 16, 16),
            Direction.WEST, Block.box(0, 10, 5, 5, 16, 11),
            Direction.EAST, Block.box(11, 10, 5, 16, 16, 11));

    public final boolean beam;
    /** Сколько блоков держит в стороны (по X и Z) в раме и в одиночку, и вверх-вниз. */
    public final int horizontal, loneHorizontal, vertical;
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public SupportBlock(Properties properties, boolean beam, int horizontal, int loneHorizontal, int vertical) {
        super(properties);
        this.beam = beam;
        this.horizontal = horizontal;
        this.loneHorizontal = loneHorizontal;
        this.vertical = vertical;
        BlockState state = stateDefinition.any();
        for (BooleanProperty arm : ARMS.values()) {
            state = state.setValue(arm, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMS.values().toArray(BooleanProperty[]::new));
    }

    /** Сколько блоков опора держит в стороны: в раме — horizontal, одна — loneHorizontal. */
    public int reach(boolean framed) {
        return framed ? horizontal : loneHorizontal;
    }

    /** Опора в pos (в раме или нет) держит блок target. */
    public boolean covers(BlockPos pos, boolean framed, BlockPos target) {
        int reach = reach(framed);
        return Math.abs(target.getX() - pos.getX()) <= reach && Math.abs(target.getZ() - pos.getZ()) <= reach
                && Math.abs(target.getY() - pos.getY()) <= vertical;
    }

    /**
     * Опора в pos — часть рамы: перекладина, у которой по одной оси с обеих сторон (через другие перекладины) стоят
     * стойки, или стойка, к столбу которой примыкает такая перекладина.
     */
    public static boolean isFramed(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof SupportBlock support)) {
            return false;
        }
        if (support.beam) {
            return isFramedBeam(level, pos);
        }
        // весь столб стоек: перекладина может примыкать к любой из них
        BlockPos bottom = pos;
        while (isPost(level.getBlockState(bottom.below()))) {
            bottom = bottom.below();
        }
        for (BlockPos p = bottom; isPost(level.getBlockState(p)); p = p.above()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos side = p.relative(dir);
                if (level.getBlockState(side).getBlock() instanceof SupportBlock other && other.beam && isFramedBeam(level, side)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isFramedBeam(BlockGetter level, BlockPos pos) {
        return postAlong(level, pos, Direction.EAST) && postAlong(level, pos, Direction.WEST)
                || postAlong(level, pos, Direction.NORTH) && postAlong(level, pos, Direction.SOUTH);
    }

    /** По прямой от перекладины через другие перекладины есть стойка. */
    private static boolean postAlong(BlockGetter level, BlockPos pos, Direction dir) {
        for (int i = 1; i <= 2 * MAX_SPAN + 1; i++) {
            BlockState next = level.getBlockState(pos.relative(dir, i));
            if (!(next.getBlock() instanceof SupportBlock support)) {
                return false;
            }
            if (!support.beam) {
                return true;
            }
        }
        return false;
    }

    /** Состояние с плечами к соседям в pos. */
    public BlockState connected(BlockGetter level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            state = state.setValue(ARMS.get(dir), connects(level.getBlockState(pos.relative(dir))));
        }
        return state;
    }

    private boolean connects(BlockState neighbor) {
        // стойка со стойкой рядом не соединяется, перекладина — со всем
        return neighbor.getBlock() instanceof SupportBlock other && (beam || other.beam);
    }

    public static boolean isPost(BlockState state) {
        return state.getBlock() instanceof SupportBlock support && !support.beam;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        if (isPost(under)) {
            return true;
        }
        if (!beam) {
            return under.isFaceSturdy(level, below, Direction.UP);
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= MAX_SPAN; i++) {
                BlockState next = level.getBlockState(pos.relative(dir, i));
                if (!(next.getBlock() instanceof SupportBlock support)) {
                    break;
                }
                if (!support.beam) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        // без опоры — сломаться в следующем тике (так рушится вся цепочка перекладин, а не только соседняя)
        if (!canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        if (dir.getAxis().isHorizontal()) {
            return state.setValue(ARMS.get(dir), connects(neighbor));
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, s -> {
            VoxelShape shape = beam ? CORE : POST;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (s.getValue(ARMS.get(dir))) {
                    shape = Shapes.or(shape, ARM_SHAPES.get(dir));
                }
            }
            return shape;
        });
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(asItem());
    }
}

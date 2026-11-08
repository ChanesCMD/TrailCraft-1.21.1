package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Забор с диагональными соединениями. Shift+ПКМ по забору выключает/включает диагонали.
 * Углы: north-east, north-west, south-east, south-west.
 */
public class DiagonalFenceBlock extends FenceBlock {
    public static final BooleanProperty ALLOW_DIAGONAL = BooleanProperty.create("allow_diagonal");
    public static final BooleanProperty NE = BooleanProperty.create("diagonal_north_east");
    public static final BooleanProperty NW = BooleanProperty.create("diagonal_north_west");
    public static final BooleanProperty SE = BooleanProperty.create("diagonal_south_east");
    public static final BooleanProperty SW = BooleanProperty.create("diagonal_south_west");

    private static final BooleanProperty[] DIAGONALS = {NE, NW, SE, SW};
    /** Смещения диагональных соседей: NE, NW, SE, SW. */
    private static final int[][] OFFSETS = {{1, -1}, {-1, -1}, {1, 1}, {-1, 1}};
    /** Противоположный угол: NE ↔ SW, NW ↔ SE. */
    private static final int[] OPPOSITE = {3, 2, 1, 0};

    public DiagonalFenceBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ALLOW_DIAGONAL, true)
                .setValue(NE, false).setValue(NW, false).setValue(SE, false).setValue(SW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ALLOW_DIAGONAL, NE, NW, SE, SW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        boolean allow = context.getPlayer() == null || !context.getPlayer().isShiftKeyDown();
        state = state.setValue(ALLOW_DIAGONAL, allow);
        if (allow) {
            BlockPos pos = context.getClickedPos();
            for (int i = 0; i < DIAGONALS.length; i++) {
                state = state.setValue(DIAGONALS[i], canConnect(context.getLevel(), pos, i));
            }
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || !state.getValue(ALLOW_DIAGONAL)) {
            return;
        }
        for (int i = 0; i < DIAGONALS.length; i++) {
            BlockPos other = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
            if (level.getBlockState(other).getBlock() instanceof DiagonalFenceBlock
                    && level.getBlockState(other).getValue(ALLOW_DIAGONAL)) {
                level.setBlock(other, level.getBlockState(other).setValue(DIAGONALS[OPPOSITE[i]], true), 3);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (int i = 0; i < DIAGONALS.length; i++) {
                BlockPos other = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
                BlockState neighbor = level.getBlockState(other);
                if (neighbor.getBlock() instanceof DiagonalFenceBlock && neighbor.getValue(DIAGONALS[OPPOSITE[i]])) {
                    level.setBlock(other, neighbor.setValue(DIAGONALS[OPPOSITE[i]], false), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean allow = !state.getValue(ALLOW_DIAGONAL);
            BlockState updated = state.setValue(ALLOW_DIAGONAL, allow);
            for (BooleanProperty property : DIAGONALS) {
                updated = updated.setValue(property, false);
            }
            level.setBlock(pos, updated, 3);
            if (allow) {
                for (int i = 0; i < DIAGONALS.length; i++) {
                    BlockPos other = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
                    if (level.getBlockState(other).getBlock() instanceof DiagonalFenceBlock
                            && level.getBlockState(other).getValue(ALLOW_DIAGONAL)) {
                        level.setBlock(pos, level.getBlockState(pos).setValue(DIAGONALS[i], true), 3);
                        level.setBlock(other, level.getBlockState(other).setValue(DIAGONALS[OPPOSITE[i]], true), 3);
                    }
                }
            } else {
                for (int i = 0; i < DIAGONALS.length; i++) {
                    BlockPos other = pos.offset(OFFSETS[i][0], 0, OFFSETS[i][1]);
                    BlockState neighbor = level.getBlockState(other);
                    if (neighbor.getBlock() instanceof DiagonalFenceBlock && neighbor.getValue(DIAGONALS[OPPOSITE[i]])) {
                        level.setBlock(other, neighbor.setValue(DIAGONALS[OPPOSITE[i]], false), 3);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean canConnect(Level level, BlockPos pos, int index) {
        BlockPos other = pos.offset(OFFSETS[index][0], 0, OFFSETS[index][1]);
        BlockState state = level.getBlockState(other);
        return state.getBlock() instanceof DiagonalFenceBlock && state.getValue(ALLOW_DIAGONAL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = super.getShape(state, level, pos, context);
        if (state.getValue(NE)) shape = Shapes.or(shape, Block.box(8, 0, 0, 16, 16, 8));
        if (state.getValue(NW)) shape = Shapes.or(shape, Block.box(0, 0, 0, 8, 16, 8));
        if (state.getValue(SE)) shape = Shapes.or(shape, Block.box(8, 0, 8, 16, 16, 16));
        if (state.getValue(SW)) shape = Shapes.or(shape, Block.box(0, 0, 8, 8, 16, 16));
        return shape;
    }
}

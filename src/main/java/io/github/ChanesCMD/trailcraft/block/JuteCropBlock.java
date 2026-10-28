package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

import javax.annotation.Nullable;

/**
 * Посаженный джут: семена сажаются только на грядку, и над ней нужен свободный блок — джут с первой стадии в два
 * блока (рисунок автора: scripts/templates/jute_stages.png). Растёт как ванильный кувшинчик (pitcher crop):
 * стадии 0–4, на 4-й цветёт и готов к сбору. Скорость — как у пшеницы
 * (влажная грядка быстрее сухой, соседние грядки помогают), жёлтозём/чернозём её меняют (GardenBedBlock).
 * Падает только с нижней половины (лут-таблица): спелый — джут и семена, неспелый — семена.
 */
public class JuteCropBlock extends DoublePlantBlock implements BonemealableBlock {
    public static final MapCodec<JuteCropBlock> CODEC = simpleCodec(JuteCropBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    public static final int MAX_AGE = 4;
    /** С этой стадии джут в два блока высотой (с первой: на рисунке все стадии выше блока). */
    private static final int DOUBLE_AGE = 0;
    private static final VoxelShape[] LOWER_SHAPES = {
            Block.box(5.0, -1.0, 5.0, 11.0, 16.0, 11.0),
            Block.box(2.0, -1.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(1.0, -1.0, 1.0, 15.0, 16.0, 15.0),
            Block.box(1.0, -1.0, 1.0, 15.0, 16.0, 15.0),
            Block.box(1.0, -1.0, 1.0, 15.0, 16.0, 15.0)};
    /** Верхняя половина — по высоте рисунка стадии. */
    private static final VoxelShape[] UPPER_SHAPES = {
            Block.box(5.0, 0.0, 5.0, 11.0, 3.0, 11.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 11.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0)};

    public JuteCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<JuteCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return UPPER_SHAPES[state.getValue(AGE)];
        }
        return LOWER_SHAPES[state.getValue(AGE)];
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos pos, BlockPos facingPos) {
        if (isDouble(state.getValue(AGE))) {
            return super.updateShape(state, facing, facingState, level, pos, facingPos);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (isLower(state) && !hasLight(level, pos)) {
            return false;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / growthSpeed(level, pos)) + 1) == 0)) {
            grow(level, state, pos);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lower = lowerHalf(level, pos, state);
        return lower != null && canGrow(level, lower, level.getBlockState(lower));
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lower = lowerHalf(level, pos, state);
        if (lower != null) {
            grow(level, level.getBlockState(lower), lower);
        }
    }

    /** Следующая стадия; с DOUBLE_AGE над джутом появляется (или растёт вместе с ним) верхняя половина. */
    private void grow(ServerLevel level, BlockState lower, BlockPos pos) {
        if (!canGrow(level, pos, lower)) {
            return;
        }
        int age = lower.getValue(AGE) + 1;
        BlockState grown = lower.setValue(AGE, age);
        level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
        if (isDouble(age)) {
            level.setBlock(pos.above(), grown.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    private boolean canGrow(LevelReader level, BlockPos pos, BlockState lower) {
        int age = lower.getValue(AGE);
        if (age >= MAX_AGE || !hasLight(level, pos)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return !isDouble(age + 1) || above.isAir() || above.is(this);
    }

    @Nullable
    private BlockPos lowerHalf(LevelReader level, BlockPos pos, BlockState state) {
        if (isLower(state)) {
            return pos;
        }
        return isLower(level.getBlockState(pos.below())) ? pos.below() : null;
    }

    private boolean isLower(BlockState state) {
        return state.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean isDouble(int age) {
        return age >= DOUBLE_AGE;
    }

    /** Как у пшеницы: растёт при свете от 8 или под открытым небом, иначе выпадает. */
    private static boolean hasLight(LevelReader level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos);
    }

    /**
     * Как у пшеницы (без штрафа за посадку рядами): грядка под джутом даёт 1, влажная — 3,
     * каждая соседняя грядка — четверть от этого.
     */
    private static float growthSpeed(BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos soil = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = soil.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                float f = s.getBlock() instanceof FarmBlock ? (s.isFertile(level, p) ? 3.0F : 1.0F) : 0.0F;
                speed += dx == 0 && dz == 0 ? f : f / 4.0F;
            }
        }
        return speed;
    }
}

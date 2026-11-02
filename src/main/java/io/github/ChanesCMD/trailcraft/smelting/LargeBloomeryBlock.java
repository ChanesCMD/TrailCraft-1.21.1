package io.github.ChanesCMD.trailcraft.smelting;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Люк большого сыродутного горна. Сам люк — передняя нижняя точка стены; вокруг него должна быть
 * полая труба 3×3×3 из укреплённых кирпичей (тег #trailcraft:bloomery_bricks), внутри — воздух.
 * ПКМ по собранной структуре открывает окно горна.
 */
public class LargeBloomeryBlock extends BaseEntityBlock {
    public static final MapCodec<LargeBloomeryBlock> CODEC = simpleCodec(LargeBloomeryBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LargeBloomeryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeBloomeryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModSmelting.LARGE_BLOOMERY_ENTITY.get(),
                LargeBloomeryBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (isStructureValid(level, pos, state) && level.getBlockEntity(pos) instanceof LargeBloomeryBlockEntity bloomery) {
                player.openMenu(bloomery);
            } else {
                player.displayClientMessage(Component.translatable("message.trailcraft.large_bloomery.invalid"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Проверка трубы: 3×3 снаружи, внутри воздух, люк — передняя нижняя точка. */
    public static boolean isStructureValid(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos center = pos.relative(facing.getOpposite());
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (dx == 0 && dz == 0) {
                        if (!level.getBlockState(p).isAir()) {
                            return false;
                        }
                    } else if (!(dy == 0 && p.equals(pos)) && !level.getBlockState(p).is(ModTags.BLOOMERY_BRICKS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LargeBloomeryBlockEntity bloomery) {
            for (int i = 0; i < bloomery.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bloomery.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.12) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        }
        level.addParticle(ParticleTypes.FLAME, x + random.nextDouble() * 0.4 - 0.2, y + 0.4,
                z + random.nextDouble() * 0.4 - 0.2, 0, 0.02, 0);
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + random.nextDouble() * 0.3 - 0.15, y + 1.1,
                z + random.nextDouble() * 0.3 - 0.15, 0, 0.04, 0);
    }
}

package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.GraveBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Могила (см. GraveBlockEntity). Чужой игрок в первые 2 часа не может ни открыть, ни сломать её — видит таймер.
 * Может стоять в воде; текущая вода и лава её не сносят (блок принудительно твёрдый, см. ModBlocks).
 */
public class GraveBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<GraveBlock> CODEC = simpleCodec(GraveBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 4, 14, 14, 12);
    /** Насколько далеко от места смерти искать место под могилу. */
    private static final int SEARCH_RADIUS = 16;

    public GraveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            if (grave.canAccess(player)) {
                player.openMenu(grave);
            } else {
                player.displayClientMessage(Component.translatable("message.trailcraft.grave.locked",
                        grave.getOwnerName(), GraveBlockEntity.formatDuration(grave.privateFor())), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Чужой игрок не может ломать могилу, пока она личная. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave && !grave.canAccess(player)) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** Владелец сломал могилу — рюкзак выпадает ему, значит он его забрал. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GraveBlockEntity grave
                && grave.isOwner(player) && grave.hasBackpack()) {
            grave.markBackpackTaken(player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            Containers.dropContents(level, pos, grave);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Ставит могилу с вещами на месте смерти, а если там занято — в ближайшем свободном месте
     * (в радиусе SEARCH_RADIUS). Что не влезло в могилу, выпадает рядом. @return позиция могилы или null, если места нет
     */
    @Nullable
    public static BlockPos place(ServerPlayer player, List<ItemStack> items, long deathTime) {
        Level level = player.level();
        BlockPos pos = findSpot(level, player.blockPosition());
        if (pos == null) {
            return null;
        }
        boolean water = level.getFluidState(pos).is(Fluids.WATER);
        level.setBlock(pos, ModBlocks.GRAVE.get().defaultBlockState().setValue(WATERLOGGED, water), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) {
            return null;
        }
        grave.setOwner(player, deathTime);
        int slot = 0;
        for (ItemStack stack : items) {
            if (slot < GraveBlockEntity.SIZE) {
                grave.setItem(slot++, stack);
            } else {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        return pos;
    }

    /** Ближайшая к точке смерти заменяемая позиция (воздух, вода, трава…): обходим кубы всё большего радиуса. */
    @Nullable
    private static BlockPos findSpot(Level level, BlockPos origin) {
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight() - 1;
        BlockPos start = new BlockPos(origin.getX(), Mth.clamp(origin.getY(), minY, maxY), origin.getZ());
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(start.offset(-r, -r, -r), start.offset(r, r, r))) {
                int shell = Math.max(Math.abs(pos.getX() - start.getX()),
                        Math.max(Math.abs(pos.getY() - start.getY()), Math.abs(pos.getZ() - start.getZ())));
                if (shell != r || pos.getY() < minY || pos.getY() > maxY || !level.getBlockState(pos).canBeReplaced()) {
                    continue;
                }
                double distance = pos.distSqr(start);
                if (distance < bestDistance) {
                    best = pos.immutable();
                    bestDistance = distance;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }
}

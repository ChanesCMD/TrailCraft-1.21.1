package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.GraveBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** Могила (см. GraveBlockEntity). Чужой игрок в первые 2 часа не может ни открыть, ни сломать её — видит таймер. */
public class GraveBlock extends BaseEntityBlock {
    public static final MapCodec<GraveBlock> CODEC = simpleCodec(GraveBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 4, 14, 14, 12);

    public GraveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
     * Ставит могилу с вещами на месте смерти (или ближайшем свободном месте выше).
     * Что не влезло в могилу, выпадает рядом. @return позиция могилы или null, если места нет
     */
    @Nullable
    public static BlockPos place(ServerPlayer player, List<ItemStack> items, long deathTime) {
        Level level = player.level();
        BlockPos start = player.blockPosition();
        int y = Mth.clamp(start.getY(), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        for (int dy = 0; dy < 16 && y + dy < level.getMaxBuildHeight(); dy++) {
            BlockPos pos = new BlockPos(start.getX(), y + dy, start.getZ());
            if (!level.getBlockState(pos).canBeReplaced()) {
                continue;
            }
            level.setBlock(pos, ModBlocks.GRAVE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
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
        }
        return null;
    }
}

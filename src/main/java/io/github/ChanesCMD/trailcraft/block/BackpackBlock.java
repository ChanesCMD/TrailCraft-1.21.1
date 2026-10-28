package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.BackpackBlockEntity;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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

/**
 * Рюкзак на земле. Появляется, когда рюкзак выбрасывают (или ставят ПКМ).
 * ПКМ — забрать: в слот рюкзака, иначе в левую руку.
 * При разрушении выпадает предметом со всем содержимым.
 */
public class BackpackBlock extends BaseEntityBlock {
    public static final MapCodec<BackpackBlock> CODEC = simpleCodec(BackpackBlock::new);
    /** По моделям автора (scripts/templates/models/*_backpack.bbmodel). */
    private static final VoxelShape SHAPE = Block.box(4, 0, 5, 12, 9, 10);
    /** Как далеко от игрока искать место для выброшенного рюкзака. */
    private static final int SEARCH_RADIUS = 3;

    public BackpackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            backpack.setStack(stack.copyWithCount(1));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            ItemStack stack = backpack.getStack();
            if (stack.isEmpty() || BackpackItem.equip(player, stack)) {
                backpack.setStack(ItemStack.EMPTY);
                level.removeBlock(pos, false);
            } else {
                player.displayClientMessage(Component.translatable("message.trailcraft.backpack.no_space"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack
                && !backpack.getStack().isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), backpack.getStack());
            backpack.setStack(ItemStack.EMPTY);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Ставит выброшенный рюкзак на землю у ног игрока, а если там занято (например, другим рюкзаком) —
     * в ближайшее свободное место на твёрдой опоре в радиусе SEARCH_RADIUS. @return false, если места нет
     */
    public static boolean placeOnGround(Level level, BlockPos feet, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return false;
        }
        BlockPos pos = findSpot(level, feet);
        if (pos == null) {
            return false;
        }
        level.setBlock(pos, item.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            backpack.setStack(stack.copyWithCount(1));
            return true;
        }
        return false;
    }

    /** Ближайшая к ногам позиция без жидкости, которую можно занять и под которой есть опора (как для факела). */
    @Nullable
    private static BlockPos findSpot(Level level, BlockPos feet) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                feet.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
            double distance = pos.distSqr(feet);
            if (distance >= bestDistance || level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            BlockPos below = pos.below();
            if (state.canBeReplaced() && state.getFluidState().isEmpty()
                    && Block.canSupportCenter(level, below, Direction.UP)) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }
}

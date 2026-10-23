package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.BackpackBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
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
 * ПКМ — забрать: в слот рюкзака, иначе в левую руку, иначе в свободный слот хотбара.
 * При разрушении выпадает предметом со всем содержимым.
 */
public class BackpackBlock extends BaseEntityBlock {
    public static final MapCodec<BackpackBlock> CODEC = simpleCodec(BackpackBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 4, 13, 12, 12);

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
            if (stack.isEmpty() || give(player, stack)) {
                backpack.setStack(ItemStack.EMPTY);
                level.removeBlock(pos, false);
            } else {
                player.displayClientMessage(Component.translatable("message.trailcraft.backpack.no_space"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    private static boolean give(Player player, ItemStack stack) {
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        if (slots.getStackInSlot(PlayerSlots.BACKPACK).isEmpty() && slots.isItemValid(PlayerSlots.BACKPACK, stack)) {
            slots.setStackInSlot(PlayerSlots.BACKPACK, stack);
            return true;
        }
        if (player.getOffhandItem().isEmpty()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, stack);
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, stack);
                return true;
            }
        }
        return false;
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

    /** Ставит выброшенный рюкзак на землю у ног игрока (или на блок выше). @return false, если места нет */
    public static boolean placeOnGround(Level level, BlockPos feet, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return false;
        }
        for (BlockPos pos : new BlockPos[]{feet, feet.above()}) {
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos).getFluidState().isEmpty()) {
                level.setBlock(pos, item.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
                    backpack.setStack(stack.copyWithCount(1));
                    return true;
                }
            }
        }
        return false;
    }
}

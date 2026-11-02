package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Каменная наковальня: на неё кладут крицу (как на депо Create), бьют каменным молотком и получают слиток.
 * Верх на 2 пикселя ниже полного блока.
 */
public class StoneAnvilBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public StoneAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModSmelting.BLOOM.get())) {
            if (!level.isClientSide) {
                stack.shrink(1);
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5,
                        new ItemStack(ModSmelting.BLOOM.get()));
                item.setNoGravity(true);
                item.setDeltaMovement(0, 0, 0);
                item.setPickUpDelay(32767);
                level.addFreshEntity(item);
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ModItems.STONE_HAMMER.get())) {
            if (!level.isClientSide) {
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                        new AABB(pos).inflate(1.0, 1.5, 1.0))) {
                    if (item.getItem().is(ModSmelting.BLOOM.get())) {
                        item.discard();
                        player.addItem(new ItemStack(Items.IRON_INGOT));
                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
                        return ItemInteractionResult.sidedSuccess(false);
                    }
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

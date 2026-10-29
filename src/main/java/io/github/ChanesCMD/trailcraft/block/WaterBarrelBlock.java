package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModFluids;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import io.github.ChanesCMD.trailcraft.water.Water;
import io.github.ChanesCMD.trailcraft.water.WaterEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Бочка — хранилище воды или танина (WaterBarrelBlockEntity): ведро воды или танина — налить 1000 mB, пустое ведро —
 * набрать, фляга — наполнить (по 100 mB за глоток), пустой рукой — попить. В танин кладут выскобленные шкуры, готовую
 * кожу забирают пустой рукой. Shift + пустая рука — что в бочке. В тэге c:barrels/wooden (жидкостный бак, хранилище Create).
 */
public class WaterBarrelBlock extends BaseEntityBlock {
    public static final MapCodec<WaterBarrelBlock> CODEC = simpleCodec(WaterBarrelBlock::new);

    public WaterBarrelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterBarrelBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WATER_BARREL.get(), WaterBarrelBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WaterBarrelBlockEntity barrel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModFluids.TANNIN.bucket.get())) {
            if (barrel.water() > 0 || barrel.tannin() + 1000 > WaterBarrelBlockEntity.CAPACITY) {
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                barrel.add(ModFluids.TANNIN.source.get(), 1000);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BUCKET) && barrel.tannin() >= 1000) {
            if (!level.isClientSide) {
                barrel.take(1000);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModFluids.TANNIN.bucket.get())));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ModItems.SCRAPED_HIDE.get()) && barrel.tannin() > 0) {
            if (!level.isClientSide) {
                int added = barrel.addHides(stack.getCount());
                if (added == 0) {
                    return ItemInteractionResult.FAIL;
                }
                stack.consume(added, player);
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6f, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.WATER_BUCKET)) {
            if (barrel.tannin() > 0 || barrel.water() + 1000 > WaterBarrelBlockEntity.CAPACITY || barrel.water() > 0 && barrel.isSalty()) {
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                barrel.add(1000, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BUCKET) && barrel.water() >= 1000 && !barrel.isSalty()) {
            if (!level.isClientSide) {
                barrel.take(1000);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FlaskItem && barrel.water() > 0 && FlaskItem.canFill(stack, barrel.isSalty())) {
            if (!level.isClientSide) {
                int sips = Math.min(Water.FLASK_SIPS - FlaskItem.water(stack), barrel.water() / WaterBarrelBlockEntity.SIP_MB);
                if (sips > 0) {
                    boolean salty = barrel.isSalty();
                    barrel.take(sips * WaterBarrelBlockEntity.SIP_MB);
                    FlaskItem.setWater(stack, FlaskItem.water(stack) + sips);
                    if (salty) {
                        stack.set(ModComponents.SALTY.get(), true);
                    }
                    level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WaterBarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (barrel.leather() > 0 && !player.isShiftKeyDown()) {
            ItemStack leather = barrel.takeLeather();
            if (!player.getInventory().add(leather)) {
                player.drop(leather, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1);
        } else if (barrel.tannin() > 0 || barrel.hides() > 0) {
            player.displayClientMessage(Component.translatable("block.trailcraft.water_barrel.tannin", barrel.tannin(),
                    WaterBarrelBlockEntity.CAPACITY, barrel.hides()), true);
        } else if (player.isShiftKeyDown() || barrel.water() < WaterBarrelBlockEntity.SIP_MB) {
            player.displayClientMessage(Component.translatable("block.trailcraft.water_barrel.amount", barrel.water(),
                    WaterBarrelBlockEntity.CAPACITY, Component.translatable(barrel.isSalty()
                            ? "block.trailcraft.water_barrel.salty" : "block.trailcraft.water_barrel.fresh")), true);
        } else if (player instanceof ServerPlayer serverPlayer && WaterEvents.drinkFromBarrel(serverPlayer, barrel.isSalty())) {
            barrel.take(WaterBarrelBlockEntity.SIP_MB);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WaterBarrelBlockEntity barrel) {
            barrel.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int water = stack.getOrDefault(ModComponents.WATER.get(), 0);
        if (stack.getOrDefault(ModComponents.TANNIN.get(), false)) {
            tooltip.add(Component.translatable("block.trailcraft.water_barrel.tannin_amount", water, WaterBarrelBlockEntity.CAPACITY)
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("block.trailcraft.water_barrel.amount", water, WaterBarrelBlockEntity.CAPACITY,
                    Component.translatable(stack.getOrDefault(ModComponents.SALTY.get(), false)
                            ? "block.trailcraft.water_barrel.salty" : "block.trailcraft.water_barrel.fresh")).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("block.trailcraft.water_barrel.desc").withStyle(ChatFormatting.GRAY));
    }
}

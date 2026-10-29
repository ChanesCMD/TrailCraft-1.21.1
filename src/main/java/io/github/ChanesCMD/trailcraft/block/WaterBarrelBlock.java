package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
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
 * Бочка — хранилище воды (WaterBarrelBlockEntity): ведро воды — налить 1000 mB (пресная), пустое ведро — набрать,
 * фляга — наполнить (по 100 mB за глоток), пустой рукой — попить. Shift + пустая рука — сколько воды. В тэге
 * c:barrels/wooden (жидкостный бак, хранилище Create).
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
        if (stack.is(Items.WATER_BUCKET)) {
            if (barrel.water() + 1000 > WaterBarrelBlockEntity.CAPACITY || barrel.water() > 0 && barrel.isSalty()) {
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
        if (player.isShiftKeyDown() || barrel.water() < WaterBarrelBlockEntity.SIP_MB) {
            player.displayClientMessage(Component.translatable("block.trailcraft.water_barrel.amount", barrel.water(),
                    WaterBarrelBlockEntity.CAPACITY, Component.translatable(barrel.isSalty()
                            ? "block.trailcraft.water_barrel.salty" : "block.trailcraft.water_barrel.fresh")), true);
        } else if (player instanceof ServerPlayer serverPlayer && WaterEvents.drinkFromBarrel(serverPlayer, barrel.isSalty())) {
            barrel.take(WaterBarrelBlockEntity.SIP_MB);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int water = stack.getOrDefault(ModComponents.WATER.get(), 0);
        tooltip.add(Component.translatable("block.trailcraft.water_barrel.amount", water, WaterBarrelBlockEntity.CAPACITY,
                Component.translatable(stack.getOrDefault(ModComponents.SALTY.get(), false)
                        ? "block.trailcraft.water_barrel.salty" : "block.trailcraft.water_barrel.fresh")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.trailcraft.water_barrel.desc").withStyle(ChatFormatting.GRAY));
    }
}

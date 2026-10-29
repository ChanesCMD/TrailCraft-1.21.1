package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import io.github.ChanesCMD.trailcraft.block.entity.CoalBurnerBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Угольная горелка — слабая горелка всполоха без всполоха (CoalBurnerBlockEntity): свойство Create heat_level, по
 * нему чаша Create сверху видит нагрев. ПКМ топливом — подбросить, пустой рукой — сколько горения осталось.
 * Котёл Create греет вчетверо слабее горелки всполоха (BOILER_HEAT; дробный нагрев складывает mixin/BoilerDataMixin).
 */
public class CoalBurnerBlock extends BaseEntityBlock {
    public static final MapCodec<CoalBurnerBlock> CODEC = simpleCodec(CoalBurnerBlock::new);
    /** Нагрев котла от горящей горелки: горелка всполоха — 1. */
    public static final float BOILER_HEAT = 0.25f;

    public CoalBurnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlazeBurnerBlock.HEAT_LEVEL);
    }

    public static boolean isLit(BlockState state) {
        return state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.NONE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalBurnerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.COAL_BURNER.get(), CoalBurnerBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (CoalBurnerBlockEntity.fuelTicks(stack) <= 0 || !(level.getBlockEntity(pos) instanceof CoalBurnerBlockEntity burner)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && burner.addFuel(stack)) {
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CoalBurnerBlockEntity burner) {
            player.displayClientMessage(Component.translatable("block.trailcraft.coal_burner.burn", burner.burn() / 20), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isLit(state) && random.nextInt(3) == 0) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0, 0.02, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.trailcraft.coal_burner.desc").withStyle(ChatFormatting.GRAY));
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = io.github.ChanesCMD.trailcraft.TrailCraft.MODID,
            bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
            event.enqueueWork(() -> com.simibubi.create.api.boiler.BoilerHeater.REGISTRY.register(
                    io.github.ChanesCMD.trailcraft.init.ModBlocks.COAL_BURNER.get(),
                    (level, pos, state) -> isLit(state) ? BOILER_HEAT : com.simibubi.create.api.boiler.BoilerHeater.NO_HEAT));
        }
    }
}

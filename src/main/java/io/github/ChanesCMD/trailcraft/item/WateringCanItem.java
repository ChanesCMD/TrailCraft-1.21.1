package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.GardenBedBlock;
import io.github.ChanesCMD.trailcraft.farm.Farm;
import io.github.ChanesCMD.trailcraft.farm.Watering;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.water.Water;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Лейка: ПКМ по пресной воде — наполнить, ПКМ по грядке (или по растению на ней) — полить квадрат грядок вокруг
 * (Farm.WATERING_RADIUS): они влажные Farm.WATERED_DAYS игровых суток (farm/Watering), потом сохнут, и растения на них
 * не растут, пока грядку снова не польют (или рядом вода, или дождь). Запас — в поливах, полоска под предметом.
 */
public class WateringCanItem extends Item {
    private final int capacity;

    public WateringCanItem(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
    }

    public int water(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModComponents.WATER.get(), 0), 0, capacity);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof GardenBedBlock)) {
            pos = pos.below();  // кликнули по растению — поливаем грядку под ним
            if (!(level.getBlockState(pos).getBlock() instanceof GardenBedBlock)) {
                return InteractionResult.PASS;
            }
        }
        ItemStack stack = context.getItemInHand();
        if (water(stack) <= 0) {
            return InteractionResult.PASS;  // пустая: дальше use() — может, игрок целится в воду
        }
        if (level instanceof ServerLevel server) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-Farm.WATERING_RADIUS, 0, -Farm.WATERING_RADIUS),
                    pos.offset(Farm.WATERING_RADIUS, 0, Farm.WATERING_RADIUS))) {
                BlockState state = server.getBlockState(p);
                if (state.getBlock() instanceof GardenBedBlock) {
                    Watering.water(server, p);
                    server.setBlock(p, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
                    server.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5, 6, 0.3, 0, 0.3, 0);
                }
            }
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                stack.set(ModComponents.WATER.get(), water(stack) - 1);
            }
            server.playSound(null, pos, SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        // только пресная вода: морской (море и его берега) растения не поливают
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)
                || Water.isSalty(level, hit.getBlockPos()) || water(stack) >= capacity) {
            return InteractionResultHolder.pass(stack);
        }
        stack.set(ModComponents.WATER.get(), capacity);
        level.playSound(player, hit.getBlockPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1f, 1f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * water(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4E81E6;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.watering_can.water", water(stack), capacity).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.trailcraft.watering_can.use", Farm.WATERED_DAYS).withStyle(ChatFormatting.GRAY));
    }
}

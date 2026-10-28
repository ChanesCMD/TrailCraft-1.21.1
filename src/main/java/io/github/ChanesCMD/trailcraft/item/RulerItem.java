package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Линейка: ПКМ по блоку — отметить начало, ПКМ по второму блоку — расстояние между ними по прямой (между центрами
 * блоков, до MAX_DISTANCE) над хотбаром и пунктир частиц между ними; Shift+ПКМ — сбросить отметку.
 * Координат не показывает — только расстояние и сдвиги по осям.
 */
public class RulerItem extends Item {
    public static final int MAX_DISTANCE = 32;
    private static final DustParticleOptions DOT = new DustParticleOptions(new Vector3f(0.95f, 0.8f, 0.2f), 0.6f);

    public RulerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        GlobalPos start = stack.get(ModComponents.RULER_START.get());
        if (player.isShiftKeyDown()) {
            stack.remove(ModComponents.RULER_START.get());
            player.displayClientMessage(Component.translatable("message.trailcraft.ruler.reset"), true);
            return InteractionResult.SUCCESS;
        }
        if (start == null || start.dimension() != level.dimension() || start.pos().equals(pos)) {
            stack.set(ModComponents.RULER_START.get(), GlobalPos.of(level.dimension(), pos));
            player.displayClientMessage(Component.translatable("message.trailcraft.ruler.start"), true);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.5f, 1.6f);
            return InteractionResult.SUCCESS;
        }
        BlockPos from = start.pos();
        int dx = pos.getX() - from.getX(), dy = pos.getY() - from.getY(), dz = pos.getZ() - from.getZ();
        double distance = Math.sqrt((double) dx * dx + dy * dy + dz * dz);
        if (distance > MAX_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.trailcraft.ruler.too_far", MAX_DISTANCE)
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        stack.remove(ModComponents.RULER_START.get());
        player.displayClientMessage(Component.translatable("message.trailcraft.ruler.distance",
                String.format("%.1f", distance), Math.abs(dx), Math.abs(dy), Math.abs(dz)), true);
        // пунктир между центрами блоков — виден только тому, кто мерил
        Vec3 a = Vec3.atCenterOf(from), b = Vec3.atCenterOf(pos);
        int dots = Math.max(2, (int) Math.ceil(distance * 3));
        for (int i = 0; i <= dots; i++) {
            Vec3 p = a.lerp(b, (double) i / dots);
            level.sendParticles(player, DOT, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 0.5f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.RULER_START.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.ruler", MAX_DISTANCE).withStyle(ChatFormatting.GRAY));
    }
}

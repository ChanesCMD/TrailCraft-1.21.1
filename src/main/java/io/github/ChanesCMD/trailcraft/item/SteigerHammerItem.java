package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.SupportBlock;
import io.github.ChanesCMD.trailcraft.collapse.Collapse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Молоток штейгера: пока он в руке, вокруг игрока видно, что обвалится. Под нависающей природной породой и рудой
 * (collapse/Collapse: под ней пусто) — точки: зелёные — держит опора, красные — может обвалиться. У опоры, на которую
 * смотрит игрок, рисуется рамка того, что она держит (голубая — в раме, жёлтая — одна), и над хотбаром — размер.
 * Видно только тому, кто держит молоток; обновляется раз в {@link #INTERVAL} тиков.
 */
public class SteigerHammerItem extends Item {
    public static final int INTERVAL = 10;
    /** Как далеко вокруг игрока отмечается порода: в стороны и вверх, вниз — DOWN. */
    public static final int RADIUS = 8, DOWN = 3;
    private static final DustParticleOptions SAFE = new DustParticleOptions(new Vector3f(0.3f, 1.0f, 0.3f), 1.2f);
    private static final DustParticleOptions UNSAFE = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.1f), 1.2f);
    private static final DustParticleOptions FRAME = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 1.0f), 0.9f);
    private static final DustParticleOptions LONE = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 0.9f);

    public SteigerHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player
                && (selected || player.getOffhandItem() == stack) && server.getGameTime() % INTERVAL == 0) {
            show(server, player);
        }
    }

    private static void show(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-RADIUS, -DOWN, -RADIUS), max = center.offset(RADIUS, RADIUS, RADIUS);
        List<Collapse.Support> supports = Collapse.supports(level, min, max);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (level.isLoaded(p) && Collapse.isNaturalRock(level, p) && FallingBlock.isFree(level.getBlockState(p.below()))) {
                level.sendParticles(player, Collapse.supported(p, supports) ? SAFE : UNSAFE, true,
                        p.getX() + 0.5, p.getY() - 0.1, p.getZ() + 0.5, 1, 0.15, 0, 0.15, 0);
            }
        }
        Vec3 eye = player.getEyePosition();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(player.blockInteractionRange())),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof SupportBlock support) {
            BlockPos pos = blockHit.getBlockPos();
            boolean framed = SupportBlock.isFramed(level, pos);
            int reach = support.reach(framed), size = reach * 2 + 1, height = support.vertical * 2 + 1;
            box(level, player, framed ? FRAME : LONE, Vec3.atLowerCornerOf(pos.offset(-reach, -support.vertical, -reach)),
                    Vec3.atLowerCornerOf(pos.offset(reach + 1, support.vertical + 1, reach + 1)));
            player.displayClientMessage(Component.translatable(framed ? "item.trailcraft.steiger_hammer.framed"
                    : "item.trailcraft.steiger_hammer.lone", size, height, size), true);
        }
    }

    /** Рёбра коробки from…to точками через полблока. */
    private static void box(ServerLevel level, ServerPlayer player, DustParticleOptions dust, Vec3 from, Vec3 to) {
        double[] xs = {from.x, to.x}, ys = {from.y, to.y}, zs = {from.z, to.z};
        for (double y : ys) {
            for (double z : zs) {
                line(level, player, dust, new Vec3(from.x, y, z), new Vec3(to.x, y, z));
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                line(level, player, dust, new Vec3(x, from.y, z), new Vec3(x, to.y, z));
            }
            for (double y : ys) {
                line(level, player, dust, new Vec3(x, y, from.z), new Vec3(x, y, to.z));
            }
        }
    }

    private static void line(ServerLevel level, ServerPlayer player, DustParticleOptions dust, Vec3 a, Vec3 b) {
        int steps = (int) Math.ceil(a.distanceTo(b) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, (double) i / steps);
            level.sendParticles(player, dust, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.trailcraft.steiger_hammer.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.trailcraft.steiger_hammer.desc2").withStyle(ChatFormatting.GRAY));
    }
}

package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Линия линейки частицами (видна только игроку с линейкой): пока в руке линейка с отмеченным началом, от него до блока под
 * прицелом (не дальше RulerItem.MAX_DISTANCE) тянется пунктир, а над хотбаром — расстояние. Измеренная линия после второго
 * клика держится ещё SHOW_TICKS.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class RulerEvents {
    private static final int PERIOD = 4, SHOW_TICKS = 200;
    private static final DustParticleOptions DOT = new DustParticleOptions(new Vector3f(0.95f, 0.8f, 0.2f), 0.6f);

    private record Shown(GlobalPos from, BlockPos to, long until) {}

    private static final Map<UUID, Shown> SHOWN = new HashMap<>();

    /** Запоминает измеренную линию, чтобы показывать её после того, как отметка сброшена. */
    public static void remember(ServerPlayer player, BlockPos from, BlockPos to) {
        SHOWN.put(player.getUUID(), new Shown(GlobalPos.of(player.level().dimension(), from), to,
                player.level().getGameTime() + SHOW_TICKS));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % PERIOD != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ItemStack ruler = holding(player);
        GlobalPos start = ruler == null ? null : ruler.get(ModComponents.RULER_START.get());
        if (start != null && start.dimension() == level.dimension()) {
            HitResult hit = player.pick(RulerItem.MAX_DISTANCE + 8, 1.0f, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                    && !block.getBlockPos().equals(start.pos())) {
                BlockPos target = block.getBlockPos();
                int dx = target.getX() - start.pos().getX(), dy = target.getY() - start.pos().getY(),
                        dz = target.getZ() - start.pos().getZ();
                double distance = Math.sqrt((double) dx * dx + dy * dy + dz * dz);
                if (distance <= RulerItem.MAX_DISTANCE) {
                    line(level, player, start.pos(), target);
                    player.displayClientMessage(Component.translatable("message.trailcraft.ruler.distance",
                            String.format("%.1f", distance), Math.abs(dx), Math.abs(dy), Math.abs(dz)), true);
                }
            }
            return;
        }
        Shown shown = SHOWN.get(player.getUUID());
        if (shown != null) {
            if (level.getGameTime() > shown.until || shown.from.dimension() != level.dimension()) {
                SHOWN.remove(player.getUUID());
            } else {
                line(level, player, shown.from.pos(), shown.to);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SHOWN.remove(event.getEntity().getUUID());
    }

    private static ItemStack holding(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof RulerItem) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof RulerItem ? player.getOffhandItem() : null;
    }

    /** Пунктир между центрами блоков, точка каждые ~0,4 блока. */
    private static void line(ServerLevel level, ServerPlayer player, BlockPos from, BlockPos to) {
        Vec3 a = Vec3.atCenterOf(from), b = Vec3.atCenterOf(to);
        int dots = Math.max(2, (int) Math.ceil(a.distanceTo(b) * 2.5));
        for (int i = 0; i <= dots; i++) {
            Vec3 p = a.lerp(b, (double) i / dots);
            level.sendParticles(player, DOT, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private RulerEvents() {}
}

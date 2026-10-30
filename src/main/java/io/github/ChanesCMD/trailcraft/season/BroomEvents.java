package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.item.BroomItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Пока игрок держит метлу, раз в полсекунды шлём ему сметённые столбцы вокруг (SweptSnowPayload) для подсветки. */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class BroomEvents {
    private static final int RADIUS = 24, PERIOD = 10, MAX_COLUMNS = 600;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % PERIOD != 0) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof BroomItem) && !(player.getOffhandItem().getItem() instanceof BroomItem)) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new SweptSnowPayload(SweptSnow.near(player.serverLevel(), player.blockPosition(), RADIUS, MAX_COLUMNS)));
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(SweptSnowPayload.TYPE, SweptSnowPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() ->
                            io.github.ChanesCMD.trailcraft.client.SweptSnowOverlay.receive(payload)));
        }
    }

    private BroomEvents() {}
}

package io.github.ChanesCMD.trailcraft.health;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.hud.FoodOverlay;
import io.github.ChanesCMD.trailcraft.mixin.FoodDataAccessor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Истощение (сколько осталось до потери насыщения или еды) ванилла клиенту не шлёт, а полоске в HUD
 * (client/hud/FoodOverlay) оно нужно: сервер отправляет его, когда оно заметно изменилось.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class FoodSync {
    private static final int INTERVAL = 5;
    private static final float STEP = 0.05f;
    private static final Map<ServerPlayer, Float> SENT = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) {
            return;
        }
        float exhaustion = ((FoodDataAccessor) player.getFoodData()).trailcraft$getExhaustion();
        Float sent = SENT.get(player);
        if (sent == null || Math.abs(sent - exhaustion) >= STEP) {
            SENT.put(player, exhaustion);
            PacketDistributor.sendToPlayer(player, new ExhaustionPayload(exhaustion));
        }
    }

    /** Сервер → клиент: текущее истощение игрока. */
    public record ExhaustionPayload(float value) implements CustomPacketPayload {
        public static final Type<ExhaustionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "exhaustion"));
        public static final StreamCodec<ByteBuf, ExhaustionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, ExhaustionPayload::value, ExhaustionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(ExhaustionPayload.TYPE, ExhaustionPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> FoodOverlay.setExhaustion(payload.value())));
        }
    }

    private FoodSync() {}
}

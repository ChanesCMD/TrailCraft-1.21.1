package io.github.ChanesCMD.trailcraft.health;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.hud.FoodOverlay;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
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
 * Истощение (сколько осталось до потери насыщения или еды) ванилла клиенту не шлёт, а полоскам в HUD
 * (client/hud/FoodOverlay) оно нужно вместе с накопленной жаждой: сервер отправляет их, когда они заметно изменились.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class FoodSync {
    private static final int INTERVAL = 5;
    private static final float STEP = 0.05f;
    private static final Map<ServerPlayer, ExhaustionPayload> SENT = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) {
            return;
        }
        ExhaustionPayload now = new ExhaustionPayload(((FoodDataAccessor) player.getFoodData()).trailcraft$getExhaustion(),
                player.getData(ModAttachments.WATER_TRACKER).thirst);
        ExhaustionPayload sent = SENT.get(player);
        if (sent == null || Math.abs(sent.food() - now.food()) >= STEP || Math.abs(sent.thirst() - now.thirst()) >= STEP / 4) {
            SENT.put(player, now);
            PacketDistributor.sendToPlayer(player, now);
        }
    }

    /** Сервер → клиент: истощение игрока (0..4) и накопленная жажда (0..1). */
    public record ExhaustionPayload(float food, float thirst) implements CustomPacketPayload {
        public static final Type<ExhaustionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "exhaustion"));
        public static final StreamCodec<ByteBuf, ExhaustionPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, ExhaustionPayload::food, ByteBufCodecs.FLOAT, ExhaustionPayload::thirst,
                        ExhaustionPayload::new);

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
                    (payload, context) -> context.enqueueWork(() -> FoodOverlay.set(payload.food(), payload.thirst())));
        }
    }

    private FoodSync() {}
}

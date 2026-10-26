package io.github.ChanesCMD.trailcraft.map;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Телепорт на точку полноэкранной карты (контекстное меню ПКМ), только для админов: тех, кому доступна команда /tp
 * (уровень прав 2). Клиент шлёт только X и Z, высоту подбирает сервер: поверхность, а в измерениях с потолком
 * (Нижний мир) — первое свободное место с опорой под ногами, если идти сверху вниз.
 */
public final class MapTeleport {
    private static final int REQUIRED_PERMISSION = 2;

    public record Payload(int x, int z) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "map_teleport"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::x, ByteBufCodecs.VAR_INT, Payload::z, Payload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(Payload.TYPE, Payload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player && player.hasPermissions(REQUIRED_PERMISSION)) {
                            teleport(player, payload.x(), payload.z());
                        }
                    }));
        }
    }

    private static void teleport(ServerPlayer player, int x, int z) {
        ServerLevel level = player.serverLevel();
        if (!level.getWorldBorder().isWithinBounds(x, z)) {
            player.sendSystemMessage(Component.translatable("message.trailcraft.map.teleport.border"));
            return;
        }
        level.getChunk(x >> 4, z >> 4); // загружает (а при необходимости и генерирует) чанк, чтобы узнать высоту
        int y = findY(level, x, z);
        player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
    }

    private static int findY(ServerLevel level, int x, int z) {
        if (!level.dimensionType().hasCeiling()) {
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 3; y > level.getMinBuildHeight(); y--) {
            BlockState below = level.getBlockState(pos.set(x, y - 1, z));
            if (below.isFaceSturdy(level, pos, Direction.UP) && free(level, pos.set(x, y, z)) && free(level, pos.set(x, y + 1, z))) {
                return y;
            }
        }
        return level.getSeaLevel();
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private MapTeleport() {}
}

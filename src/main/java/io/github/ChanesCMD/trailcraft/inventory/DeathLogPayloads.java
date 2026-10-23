package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Журнал смертей: клиент просит, сервер присылает записи и своё текущее время (статус рюкзака считается по нему). */
public final class DeathLogPayloads {
    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "death_log_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Log(List<DeathRecord> records, long now) implements CustomPacketPayload {
        public static final Type<Log> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "death_log"));
        public static final StreamCodec<ByteBuf, Log> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.fromCodec(DeathRecord.CODEC.listOf()), Log::records,
                ByteBufCodecs.VAR_LONG, Log::now,
                Log::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private DeathLogPayloads() {}
}

package io.github.ChanesCMD.trailcraft.voice;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/** Пакеты голосового чата: выбор канала, звонки и состояние для окна. */
public final class VoicePayloads {

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "voice_" + name));
    }

    /** Клиент → сервер: переключиться на канал. */
    public record SetChannel(String channel) implements CustomPacketPayload {
        public static final Type<SetChannel> TYPE = typeOf("set_channel");
        public static final StreamCodec<ByteBuf, SetChannel> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetChannel::channel, SetChannel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: позвонить игроку. */
    public record Call(UUID target) implements CustomPacketPayload {
        public static final Type<Call> TYPE = typeOf("call");
        public static final StreamCodec<ByteBuf, Call> STREAM_CODEC =
                StreamCodec.composite(UUIDUtil.STREAM_CODEC, Call::target, Call::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: ответ на входящий звонок. */
    public record Answer(boolean accept) implements CustomPacketPayload {
        public static final Type<Answer> TYPE = typeOf("answer");
        public static final StreamCodec<ByteBuf, Answer> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, Answer::accept, Answer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: завершить звонок. */
    public record Hangup() implements CustomPacketPayload {
        public static final Hangup INSTANCE = new Hangup();
        public static final Type<Hangup> TYPE = typeOf("hangup");
        public static final StreamCodec<ByteBuf, Hangup> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Сервер → клиент: вам звонят. */
    public record Incoming(UUID caller, String name) implements CustomPacketPayload {
        public static final Type<Incoming> TYPE = typeOf("incoming");
        public static final StreamCodec<ByteBuf, Incoming> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Incoming::caller, ByteBufCodecs.STRING_UTF8, Incoming::name, Incoming::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Сервер → клиент: сколько игроков в каналах party/city/country и с кем идёт звонок. */
    public record Status(int party, int city, int country, Optional<UUID> callPartner) implements CustomPacketPayload {
        public static final Status EMPTY = new Status(0, 0, 0, Optional.empty());
        public static final Type<Status> TYPE = typeOf("status");
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Status::party, ByteBufCodecs.VAR_INT, Status::city, ByteBufCodecs.VAR_INT, Status::country,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Status::callPartner, Status::new);

        public int count(VoiceChannel channel) {
            return switch (channel) {
                case PARTY -> party;
                case CITY -> city;
                case COUNTRY -> country;
                case LOCAL -> 0;
            };
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private VoicePayloads() {}
}

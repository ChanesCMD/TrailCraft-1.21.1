package io.github.ChanesCMD.trailcraft.chat;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Пакеты каналов чата (ТЗ, раздел 5). */
public final class ChatPayloads {

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "chat_" + name));
    }

    /** Клиент → сервер: сообщение в канал (кроме global — он идёт обычным чатом). */
    public record Send(String channel, String text) implements CustomPacketPayload {
        public static final Type<Send> TYPE = typeOf("send");
        public static final StreamCodec<ByteBuf, Send> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Send::channel, ByteBufCodecs.stringUtf8(256), Send::text, Send::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Сервер → клиент: сообщение канала; клиент не показывает его, если канал у себя скрыл. */
    public record Message(String channel, Component text) implements CustomPacketPayload {
        public static final Type<Message> TYPE = typeOf("message");
        public static final StreamCodec<RegistryFriendlyByteBuf, Message> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Message::channel, ComponentSerialization.STREAM_CODEC, Message::text, Message::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Сервер → клиент: дополнительные каналы и флаг голосовых устройств (при входе и после правок). */
    public record Channels(List<String> extras, boolean voiceRequiresDevice) implements CustomPacketPayload {
        public static final Type<Channels> TYPE = typeOf("channels");
        public static final StreamCodec<ByteBuf, Channels> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Channels::extras,
                ByteBufCodecs.BOOL, Channels::voiceRequiresDevice, Channels::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер (только операторы): добавить или удалить канал, переключить флаг голосовых устройств. */
    public record Edit(Action action, String value) implements CustomPacketPayload {
        public enum Action { ADD, REMOVE, VOICE_REQUIRES_DEVICE }

        public static final Type<Edit> TYPE = typeOf("edit");
        public static final StreamCodec<ByteBuf, Edit> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], Action::ordinal), Edit::action,
                ByteBufCodecs.STRING_UTF8, Edit::value, Edit::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ChatPayloads() {}
}

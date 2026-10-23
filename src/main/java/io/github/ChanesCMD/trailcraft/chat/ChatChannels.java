package io.github.ChanesCMD.trailcraft.chat;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.groups.Groups;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Каналы текстового чата (ТЗ 5.1–5.2). global идёт обычным чатом Minecraft; остальные — нашим пакетом:
 * дополнительные каналы (trading и т. п.) видят все, party/country/city — только своя группа (Groups).
 * Список дополнительных каналов и флаг голосовых устройств — в серверном конфиге, менять могут операторы.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class ChatChannels {
    public static final String GLOBAL = "global";
    /** Уровень прав для настроек чатов сервера (оператор). */
    public static final int EDIT_PERMISSION = 2;

    /** Все каналы в порядке вкладок: global, дополнительные из конфига, party, country, city. */
    public static List<String> all(List<String> extras) {
        List<String> list = new ArrayList<>();
        list.add(GLOBAL);
        list.addAll(extras);
        list.addAll(List.of("party", "country", "city"));
        return list;
    }

    public static boolean isRequired(String channel) {
        return ServerConfig.REQUIRED_CHANNELS.contains(channel);
    }

    @Nullable
    private static Groups.Kind kind(String channel) {
        return switch (channel) {
            case "party" -> Groups.Kind.PARTY;
            case "country" -> Groups.Kind.COUNTRY;
            case "city" -> Groups.Kind.CITY;
            default -> null;
        };
    }

    private static void send(ServerPlayer sender, String channel, String raw) {
        String text = StringUtil.trimChatMessage(raw.strip());
        if (text.isEmpty() || channel.equals(GLOBAL) || !all(ServerConfig.extraChannels()).contains(channel)) {
            return;
        }
        Groups.Kind kind = kind(channel);
        List<ServerPlayer> recipients = kind == null ? sender.server.getPlayerList().getPlayers() : Groups.members(sender, kind);
        if (recipients.isEmpty()) {
            sender.sendSystemMessage(Component.translatable("message.trailcraft.chat.no_group", channel));
            return;
        }
        Component message = Component.translatable("chat.trailcraft.channel", channel, sender.getDisplayName(), text);
        TrailCraft.LOGGER.info("[{}] <{}> {}", channel, sender.getGameProfile().getName(), text);
        for (ServerPlayer recipient : recipients) {
            PacketDistributor.sendToPlayer(recipient, new ChatPayloads.Message(channel, message));
        }
    }

    private static void edit(ServerPlayer player, ChatPayloads.Edit edit) {
        if (!player.hasPermissions(EDIT_PERMISSION)) {
            return;
        }
        List<String> extras = new ArrayList<>(ServerConfig.extraChannels());
        switch (edit.action()) {
            case ADD -> {
                String name = edit.value().strip().toLowerCase();
                if (!ServerConfig.validChannel(name) || extras.contains(name)) {
                    player.sendSystemMessage(Component.translatable("message.trailcraft.chat.bad_name"));
                    return;
                }
                extras.add(name);
                ServerConfig.EXTRA_CHANNELS.set(extras);
            }
            case REMOVE -> {
                extras.remove(edit.value());
                ServerConfig.EXTRA_CHANNELS.set(extras);
            }
            case VOICE_REQUIRES_DEVICE -> ServerConfig.VOICE_REQUIRES_DEVICE.set(Boolean.parseBoolean(edit.value()));
        }
        ServerConfig.SPEC.save();
        PacketDistributor.sendToAllPlayers(state());
    }

    private static ChatPayloads.Channels state() {
        return new ChatPayloads.Channels(ServerConfig.extraChannels(), ServerConfig.voiceRequiresDevice());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, state());
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(ChatPayloads.Send.TYPE, ChatPayloads.Send.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            send(player, payload.channel(), payload.text());
                        }
                    }));
            registrar.playToServer(ChatPayloads.Edit.TYPE, ChatPayloads.Edit.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            edit(player, payload);
                        }
                    }));
            registrar.playToClient(ChatPayloads.Message.TYPE, ChatPayloads.Message.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> io.github.ChanesCMD.trailcraft.client.chat.ChatClient.receive(payload)));
            registrar.playToClient(ChatPayloads.Channels.TYPE, ChatPayloads.Channels.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> io.github.ChanesCMD.trailcraft.client.chat.ChatClient.channels(payload)));
        }
    }

    private ChatChannels() {}
}

package io.github.ChanesCMD.trailcraft.voice;

import com.mojang.brigadier.context.CommandContext;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.groups.Groups;
import io.github.ChanesCMD.trailcraft.voice.svc.SvcServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Голосовой чат на сервере поверх Simple Voice Chat (ТЗ, раздел 7): каналы party/city/country — скрытые группы SVC,
 * local — без группы; звонок — изолированная группа на двоих после подтверждения. Без мода voicechat всё выключено.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class VoiceServer {
    /** Сколько тиков ждём ответа на звонок. TODO: уточнить. */
    public static final int RING_TICKS = 30 * 20;
    private static final int STATUS_INTERVAL = 40;
    private static final boolean LOADED = ModList.get().isLoaded("voicechat");

    /** Кому звонят → кто звонит, и когда начался вызов. */
    private static final Map<UUID, UUID> RINGING = new HashMap<>();
    private static final Map<UUID, Long> RING_STARTED = new HashMap<>();
    /** Собеседники в звонке (в обе стороны). */
    private static final Map<UUID, UUID> CALLS = new HashMap<>();

    public static boolean available() {
        return LOADED && SvcServer.ready();
    }

    private static void message(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.trailcraft.voice." + key, args));
    }

    /** Проверки, общие для каналов и звонков. */
    private static boolean usable(ServerPlayer player) {
        if (!available()) {
            message(player, "not_installed");
            return false;
        }
        if (!SvcServer.isConnected(player)) {
            message(player, "not_connected");
            return false;
        }
        return true;
    }

    // === Каналы ===

    public static void setChannel(ServerPlayer player, String id) {
        VoiceChannel channel;
        try {
            channel = VoiceChannel.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!usable(player)) {
            return;
        }
        if (channel.remote() && !CommDevices.allowsRemoteVoice(player)) {
            message(player, "need_device");
            return;
        }
        String groupId = channel.kind == null ? null : Groups.of(player, channel.kind);
        if (channel.kind != null && groupId == null) {
            message(player, "no_group", channel.id());
            return;
        }
        hangup(player);
        if (groupId == null) {
            SvcServer.leaveGroup(player);
        } else {
            SvcServer.joinGroup(player, channel.groupName(groupId), false);
        }
        sendStatus(player);
    }

    // === Звонки ===

    public static void call(ServerPlayer caller, UUID targetId) {
        if (!usable(caller) || caller.getUUID().equals(targetId)) {
            return;
        }
        ServerPlayer target = caller.server.getPlayerList().getPlayer(targetId);
        if (!CommDevices.allowsRemoteVoice(caller)) {
            message(caller, "need_device");
        } else if (target == null || !SvcServer.isConnected(target)) {
            message(caller, "target_offline");
        } else if (!CommDevices.allowsRemoteVoice(target)) {
            message(caller, "target_no_device", target.getDisplayName());
        } else if (CALLS.containsKey(targetId) || RINGING.containsKey(targetId)) {
            message(caller, "busy", target.getDisplayName());
        } else {
            hangup(caller);
            RINGING.put(targetId, caller.getUUID());
            RING_STARTED.put(targetId, caller.server.overworld().getGameTime());
            message(caller, "calling", target.getDisplayName());
            target.sendSystemMessage(Component.translatable("message.trailcraft.voice.incoming", caller.getDisplayName(),
                    button("accept", "/call accept", ChatFormatting.GREEN), button("decline", "/call decline", ChatFormatting.RED)));
            PacketDistributor.sendToPlayer(target, new VoicePayloads.Incoming(caller.getUUID(), caller.getGameProfile().getName()));
        }
    }

    private static Component button(String key, String command, ChatFormatting color) {
        return Component.translatable("gui.trailcraft.call." + key)
                .withStyle(Style.EMPTY.withColor(color).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    public static void answer(ServerPlayer target, boolean accept) {
        UUID callerId = RINGING.remove(target.getUUID());
        RING_STARTED.remove(target.getUUID());
        ServerPlayer caller = callerId == null ? null : target.server.getPlayerList().getPlayer(callerId);
        if (caller == null) {
            message(target, "no_call");
            return;
        }
        if (!accept) {
            message(caller, "declined", target.getDisplayName());
            message(target, "call_ended");
            return;
        }
        if (!usable(target) || !SvcServer.isConnected(caller)) {
            message(caller, "target_offline");
            return;
        }
        hangup(target);
        String group = VoiceChannel.CALL_GROUP + ":" + caller.getUUID();
        SvcServer.joinGroup(caller, group, true);
        SvcServer.joinGroup(target, group, true);
        CALLS.put(caller.getUUID(), target.getUUID());
        CALLS.put(target.getUUID(), caller.getUUID());
        message(caller, "call_started", target.getDisplayName());
        message(target, "call_started", caller.getDisplayName());
        sendStatus(caller);
        sendStatus(target);
    }

    /** Завершить звонок игрока (если он есть) и отменить его исходящий вызов. Оба возвращаются в local. */
    public static void hangup(ServerPlayer player) {
        RINGING.values().removeIf(callerId -> callerId.equals(player.getUUID()));
        UUID partnerId = CALLS.remove(player.getUUID());
        if (partnerId == null) {
            return;
        }
        CALLS.remove(partnerId);
        SvcServer.leaveGroup(player);
        message(player, "call_ended");
        sendStatus(player);
        ServerPlayer partner = player.server.getPlayerList().getPlayer(partnerId);
        if (partner != null) {
            SvcServer.leaveGroup(partner);
            message(partner, "call_ended");
            sendStatus(partner);
        }
    }

    // === Состояние для окна ===

    private static void sendStatus(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new VoicePayloads.Status(count(player, Groups.Kind.PARTY),
                count(player, Groups.Kind.CITY), count(player, Groups.Kind.COUNTRY), Optional.ofNullable(CALLS.get(player.getUUID()))));
    }

    /** Сколько игроков группы игрока подключены к голосу (0, если он не в группе). */
    private static int count(ServerPlayer player, Groups.Kind kind) {
        return (int) Groups.members(player, kind).stream().filter(SvcServer::isConnected).count();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (!available() || now % 20 != 0) {
            return;
        }
        // Не ответили вовремя
        RING_STARTED.entrySet().removeIf(entry -> {
            if (now - entry.getValue() < RING_TICKS) {
                return false;
            }
            UUID callerId = RINGING.remove(entry.getKey());
            ServerPlayer caller = callerId == null ? null : server.getPlayerList().getPlayer(callerId);
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            if (caller != null && target != null) {
                message(caller, "no_answer", target.getDisplayName());
            }
            return true;
        });
        // Собеседник отключил голосовой чат — звонок окончен
        for (UUID id : CALLS.keySet().toArray(UUID[]::new)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && (!SvcServer.isConnected(player) || !SvcServer.isConnected(CALLS.getOrDefault(id, id)))) {
                hangup(player);
            }
        }
        if (now % STATUS_INTERVAL == 0) {
            server.getPlayerList().getPlayers().forEach(VoiceServer::sendStatus);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            hangup(player);
            RINGING.remove(player.getUUID());
            RING_STARTED.remove(player.getUUID());
        }
    }

    /** /call accept | decline | end — для кнопок в сообщении о звонке. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("call")
                .then(Commands.literal("accept").executes(ctx -> run(ctx, player -> answer(player, true))))
                .then(Commands.literal("decline").executes(ctx -> run(ctx, player -> answer(player, false))))
                .then(Commands.literal("end").executes(ctx -> run(ctx, VoiceServer::hangup))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, java.util.function.Consumer<ServerPlayer> action) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            action.accept(player);
        }
        return 1;
    }

    @Nullable
    public static UUID callPartner(ServerPlayer player) {
        return CALLS.get(player.getUUID());
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(VoicePayloads.SetChannel.TYPE, VoicePayloads.SetChannel.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            setChannel(player, payload.channel());
                        }
                    }));
            registrar.playToServer(VoicePayloads.Call.TYPE, VoicePayloads.Call.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            call(player, payload.target());
                        }
                    }));
            registrar.playToServer(VoicePayloads.Answer.TYPE, VoicePayloads.Answer.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            answer(player, payload.accept());
                        }
                    }));
            registrar.playToServer(VoicePayloads.Hangup.TYPE, VoicePayloads.Hangup.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            hangup(player);
                        }
                    }));
            registrar.playToClient(VoicePayloads.Incoming.TYPE, VoicePayloads.Incoming.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> io.github.ChanesCMD.trailcraft.client.voice.VoiceClient.incoming(payload)));
            registrar.playToClient(VoicePayloads.Status.TYPE, VoicePayloads.Status.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> io.github.ChanesCMD.trailcraft.client.voice.VoiceClient.status(payload)));
        }
    }
}

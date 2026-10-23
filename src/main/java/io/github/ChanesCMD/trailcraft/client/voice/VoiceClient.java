package io.github.ChanesCMD.trailcraft.client.voice;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.CallScreen;
import io.github.ChanesCMD.trailcraft.voice.VoiceChannel;
import io.github.ChanesCMD.trailcraft.voice.VoicePayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Голосовой чат на клиенте: кто подключён и кто говорит (из Simple Voice Chat), последние говорившие,
 * текущий канал и состояние от сервера. Без мода voicechat всё «недоступно», головы не показываются.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class VoiceClient {
    /** Сколько голов показывать (ТЗ, разделы 2 и 4). */
    public static final int HEADS = 5;
    private static final boolean LOADED = ModList.get().isLoaded("voicechat");

    public enum State { TALKING, IDLE, DISABLED }

    private static VoicePayloads.Status status = VoicePayloads.Status.EMPTY;
    /** Говорившие, последний — первым. */
    private static final LinkedList<UUID> RECENT = new LinkedList<>();

    public static boolean available() {
        return LOADED && SvcClient.ready();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!available()) {
            return;
        }
        for (UUID id : connectedPlayers()) {
            if (state(id) == State.TALKING && !id.equals(RECENT.peekFirst())) {
                RECENT.remove(id);
                RECENT.addFirst(id);
            }
        }
        while (RECENT.size() > 16) {
            RECENT.removeLast();
        }
    }

    private static boolean isSelf(UUID id) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getUUID().equals(id);
    }

    /** Игроки онлайн, подключённые к голосовому чату (вместе с нами). */
    public static List<UUID> connectedPlayers() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (!available() || connection == null) {
            return List.of();
        }
        return connection.getOnlinePlayerIds().stream().filter(id -> SvcClient.isConnected(id, isSelf(id))).toList();
    }

    /** Головы показываем, только когда в голосовом чате хотя бы двое. */
    public static boolean showHeads() {
        return connectedPlayers().size() >= 2;
    }

    /** До пяти голов: сначала последние говорившие, потом остальные подключённые. */
    public static List<UUID> heads() {
        List<UUID> connected = connectedPlayers();
        List<UUID> result = new ArrayList<>();
        for (UUID id : RECENT) {
            if (connected.contains(id) && result.size() < HEADS) {
                result.add(id);
            }
        }
        for (UUID id : connected) {
            if (!result.contains(id) && result.size() < HEADS) {
                result.add(id);
            }
        }
        return result;
    }

    public static State state(UUID id) {
        boolean self = isSelf(id);
        if (SvcClient.isTalking(id, self)) {
            return State.TALKING;
        }
        return SvcClient.isDisabled(id, self) ? State.DISABLED : State.IDLE;
    }

    /** Текущий канал; null — идёт звонок или игрок в чужой группе Simple Voice Chat. */
    @Nullable
    public static VoiceChannel currentChannel() {
        return available() ? VoiceChannel.byGroupName(SvcClient.groupName()) : null;
    }

    /** Сколько игроков в канале: local — подключённые в пределах слышимости (вместе с нами), остальные — от сервера. */
    public static int count(VoiceChannel channel) {
        if (channel != VoiceChannel.LOCAL) {
            return status.count(channel);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0;
        }
        double distance = SvcClient.voiceDistance();
        List<UUID> connected = connectedPlayers();
        return (int) mc.level.players().stream()
                .filter(p -> connected.contains(p.getUUID()) && p.distanceTo(mc.player) <= distance).count();
    }

    @Nullable
    public static UUID callPartner() {
        return status.callPartner().orElse(null);
    }

    // === Пакеты от сервера ===

    public static void status(VoicePayloads.Status newStatus) {
        status = newStatus;
    }

    /** Входящий звонок: окно «Принять / Отклонить», если сейчас не открыт другой экран (иначе — кнопки в чате). */
    public static void incoming(VoicePayloads.Incoming call) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new CallScreen(call.caller(), call.name()));
        }
    }

    private VoiceClient() {}
}

package io.github.ChanesCMD.trailcraft.voice.svc;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/** Серверная часть Simple Voice Chat: подключения игроков и группы (наши каналы и звонки). */
public final class SvcServer {

    public static boolean ready() {
        return TrailVoicePlugin.serverApi != null;
    }

    @Nullable
    private static VoicechatConnection connection(ServerPlayer player) {
        VoicechatServerApi api = TrailVoicePlugin.serverApi;
        return api == null ? null : api.getConnectionOf(player.getUUID());
    }

    /** Подключён ли игрок к голосовому чату. Выключенный звук не мешает выбирать канал и звонить. */
    public static boolean isConnected(ServerPlayer player) {
        VoicechatConnection connection = connection(player);
        return connection != null && connection.isConnected();
    }

    /**
     * Перевести игрока в группу с этим именем (создаётся при первом входе, исчезает, когда опустеет).
     * Группы скрытые: в меню групп Simple Voice Chat их не видно, войти можно только через наше окно.
     * isolated — слышно только участников (звонки); иначе ещё и тех, кто рядом.
     */
    public static void joinGroup(ServerPlayer player, String name, boolean isolated) {
        VoicechatServerApi api = TrailVoicePlugin.serverApi;
        VoicechatConnection connection = connection(player);
        if (api == null || connection == null) {
            return;
        }
        Group group = api.getGroups().stream().filter(g -> g.getName().equals(name)).findFirst()
                .orElseGet(() -> api.groupBuilder().setName(name).setPersistent(false).setHidden(true)
                        .setType(isolated ? Group.Type.ISOLATED : Group.Type.OPEN).build());
        connection.setGroup(group);
    }

    /** Выйти из группы — обратно в обычный голос рядом (local). */
    public static void leaveGroup(ServerPlayer player) {
        VoicechatConnection connection = connection(player);
        if (connection != null && connection.isInGroup()) {
            connection.setGroup(null);
        }
    }

    /** Имя группы игрока или null. */
    @Nullable
    public static String groupName(ServerPlayer player) {
        VoicechatConnection connection = connection(player);
        Group group = connection == null ? null : connection.getGroup();
        return group == null ? null : group.getName();
    }

    /** Подключён ли к голосовому чату игрок с этим UUID (для проверки собеседника). */
    public static boolean isConnected(UUID player) {
        VoicechatServerApi api = TrailVoicePlugin.serverApi;
        VoicechatConnection connection = api == null ? null : api.getConnectionOf(player);
        return connection != null && connection.isConnected();
    }

    private SvcServer() {}
}

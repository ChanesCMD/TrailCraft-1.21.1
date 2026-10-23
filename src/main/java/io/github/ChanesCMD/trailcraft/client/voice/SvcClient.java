package io.github.ChanesCMD.trailcraft.client.voice;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import io.github.ChanesCMD.trailcraft.voice.svc.TrailVoicePlugin;

import javax.annotation.Nullable;
import java.util.UUID;

/** Клиентская часть Simple Voice Chat. Вызывать только через VoiceClient, когда мод voicechat загружен. */
final class SvcClient {

    /** Клиент подключён к голосовому чату сервера. */
    static boolean ready() {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        return api != null && !api.isDisconnected();
    }

    /** Игрок подключён к голосовому чату; self — это мы сами (API ждёт null). */
    static boolean isConnected(UUID player, boolean self) {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        return api != null && !api.isDisconnected(self ? null : player);
    }

    static boolean isTalking(UUID player, boolean self) {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        return api != null && api.isTalking(self ? null : player);
    }

    /** Голос выключен (звук отключён) или, для себя, микрофон заглушён. */
    static boolean isDisabled(UUID player, boolean self) {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        return api != null && (api.isDisabled(self ? null : player) || self && api.isMuted());
    }

    @Nullable
    static String groupName() {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        Group group = api == null ? null : api.getGroup();
        return group == null ? null : group.getName();
    }

    static double voiceDistance() {
        VoicechatClientApi api = TrailVoicePlugin.clientApi;
        return api == null ? 0 : api.getVoiceChatDistance();
    }

    private SvcClient() {}
}

package io.github.ChanesCMD.trailcraft.voice.svc;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import io.github.ChanesCMD.trailcraft.TrailCraft;

/**
 * Плагин Simple Voice Chat (его находит сам SVC по аннотации). Только запоминает API сервера и клиента.
 * Классы из пакета svc трогаем, лишь проверив, что мод voicechat загружен (VoiceServer / VoiceClient).
 */
@ForgeVoicechatPlugin
public class TrailVoicePlugin implements VoicechatPlugin {
    public static volatile VoicechatServerApi serverApi;
    public static volatile VoicechatClientApi clientApi;

    @Override
    public String getPluginId() {
        return TrailCraft.MODID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> serverApi = event.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> serverApi = null);
        registration.registerEvent(ClientVoicechatInitializationEvent.class, event -> clientApi = event.getVoicechat());
        registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> clientApi = event.getVoicechat());
    }
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.stamina.StaminaEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Без выносливости бег запрещён. Бег включает клиент, поэтому отпускаем клавишу бега и сбрасываем бег здесь;
 * сервер дополнительно сбрасывает его у себя. TODO: двойное нажатие «вперёд» может на тик включить бег.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientStaminaEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null && !player.isCreative() && !player.isSpectator() && StaminaEvents.get(player) <= 0) {
            minecraft.options.keySprint.setDown(false);
            player.setSprinting(false);
        }
    }
}

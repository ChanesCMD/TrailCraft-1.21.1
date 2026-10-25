package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Клавиша карты (по умолчанию M — «Ь» в русской раскладке): открывает полноэкранную карту, если в рюкзаке атлас. */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class MapKeys {
    public static final KeyMapping MAP = new KeyMapping("key.trailcraft.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
            "key.categories.trailcraft");

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (MAP.consumeClick()) {
            if (mc.screen == null) {
                MapScreen.open();
            }
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(MAP);
        }
    }

    private MapKeys() {}
}

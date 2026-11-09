package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Тестовая тёмная ночь: туман + затемняющий слой над миром, GUI остаётся ярким. */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class NightDarknessEvents {
    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        float night = strength();
        if (night <= 0f) {
            return;
        }
        float k = 1f - 0.6f * night;
        event.setRed(event.getRed() * k);
        event.setGreen(event.getGreen() * k);
        event.setBlue(event.getBlue() * k);
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre event) {
        float night = strength();
        if (night <= 0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int alpha = Math.round(255f * 0.5f * night);
        if (alpha > 0) {
            g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), alpha << 24);
        }
    }

    private static float strength() {
        Minecraft mc = Minecraft.getInstance();
        return NightDarkness.factor(mc.level) * ClientConfig.nightDarkness();
    }

    private NightDarknessEvents() {}
}

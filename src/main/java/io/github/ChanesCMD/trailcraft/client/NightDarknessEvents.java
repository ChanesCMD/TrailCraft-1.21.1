package io.github.ChanesCMD.trailcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Тестовая тёмная ночь: туман + затемняющий слой над миром, GUI остаётся ярким. */
public final class NightDarknessEvents {
    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        float night = strength();
        float[] tint = SeasonSky.tint(Minecraft.getInstance().level);
        float k = 1f - 0.6f * night;
        event.setRed(event.getRed() * k * tint[0]);
        event.setGreen(event.getGreen() * k * tint[1]);
        event.setBlue(event.getBlue() * k * tint[2]);
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float night = strength();
        GuiGraphics g = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        // сезонный оттенок: днём слабый, ночью чуть плотнее
        float[] tint = SeasonSky.tint(mc.level);
        int tintAlpha = night > 0f ? 24 : 16;
        g.fill(0, 0, width, height, (tintAlpha << 24)
                | ((int) (tint[0] * 255) << 16) | ((int) (tint[1] * 255) << 8) | (int) (tint[2] * 255));
        int alpha = Math.round(255f * 0.5f * night);
        if (alpha > 0) {
            g.fill(0, 0, width, height, alpha << 24);
        }
    }

    private static float strength() {
        Minecraft mc = Minecraft.getInstance();
        return NightDarkness.factor(mc.level) * ClientConfig.nightDarkness();
    }

    private NightDarknessEvents() {}
}

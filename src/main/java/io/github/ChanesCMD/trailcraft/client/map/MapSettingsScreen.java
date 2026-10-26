package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Настройки карты. Пока один параметр — размер миникарты; миникарта в углу экрана за окном меняется сразу.
 * Значения лежат в ClientConfig и сохраняются при закрытии окна.
 */
public class MapSettingsScreen extends Screen {
    private static final int PANEL_W = 200, PANEL_H = 92, PAD = 10;

    private final Screen parent;

    public MapSettingsScreen(Screen parent) {
        super(Component.translatable("gui.trailcraft.map.settings.title"));
        this.parent = parent;
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return (height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int x = panelX() + PAD, y = panelY();
        SizeSlider slider = addRenderableWidget(new SizeSlider(x, y + 26, PANEL_W - 2 * PAD));
        int half = (PANEL_W - 2 * PAD - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.map.settings.reset"), b -> {
                    ClientConfig.setMinimapSize(ClientConfig.MINIMAP_DEFAULT);
                    slider.sync();
                })
                .bounds(x, y + 52, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.map.settings.done"), b -> onClose())
                .bounds(x + half + 4, y + 52, half, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Без размытия мира: миникарта в углу экрана видна чётко, и размер меняется у неё на глазах
        g.fill(0, 0, width, height, 0x60000000);
        UiDraw.panel(g, panelX(), panelY(), PANEL_W, PANEL_H);
        g.drawString(font, title, panelX() + PAD, panelY() + 10, UiDraw.LABEL, false);
    }

    @Override
    public void onClose() {
        ClientConfig.SPEC.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Ползунок размера миникарты: значение пишется в настройки по ходу движения. */
    private static final class SizeSlider extends AbstractSliderButton {
        SizeSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), toSlider(ClientConfig.minimapSize()));
            updateMessage();
        }

        private static double toSlider(int size) {
            return (size - ClientConfig.MINIMAP_MIN) / (double) (ClientConfig.MINIMAP_MAX - ClientConfig.MINIMAP_MIN);
        }

        /** Размер изменили снаружи (кнопка «По умолчанию») — ползунок следует за ним. */
        void sync() {
            value = toSlider(ClientConfig.minimapSize());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.trailcraft.map.settings.size", ClientConfig.minimapSize()));
        }

        @Override
        protected void applyValue() {
            ClientConfig.setMinimapSize(ClientConfig.MINIMAP_MIN
                    + (int) Math.round(value * (ClientConfig.MINIMAP_MAX - ClientConfig.MINIMAP_MIN)));
            updateMessage();
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.client.ClientConfig.Corner;
import io.github.ChanesCMD.trailcraft.client.ClientConfig.DateMode;
import io.github.ChanesCMD.trailcraft.client.ClientConfig.Zoom;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Настройки карты (шестерёнка на полноэкранной карте): миникарта — вкл/выкл, угол экрана, размер, масштаб, непрозрачность,
 * поворот по взгляду, биом, координаты и что показывать из календаря (дата, время). Миникарта в углу экрана за окном меняется сразу.
 * Значения лежат в ClientConfig и сохраняются при закрытии окна.
 */
public class MapSettingsScreen extends Screen {
    private static final int COLUMN_W = 130, GAP = 6, PAD = 10, HEAD = 26, ROW = 24;
    private static final int PANEL_W = 2 * COLUMN_W + GAP + 2 * PAD, PANEL_H = HEAD + 6 * ROW + 4;

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
        int left = panelX() + PAD, right = left + COLUMN_W + GAP, y = panelY() + HEAD;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.minimapEnabled())
                .create(left, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.enabled"),
                        (button, value) -> ClientConfig.MINIMAP_ENABLED.set(value)));
        addRenderableWidget(CycleButton.<Corner>builder(corner -> Component.translatable("gui.trailcraft.map.settings.corner." + corner.name().toLowerCase(Locale.ROOT)))
                .withValues(Corner.values()).withInitialValue(ClientConfig.minimapCorner())
                .create(right, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.corner"),
                        (button, value) -> ClientConfig.MINIMAP_CORNER.set(value)));

        y += ROW;
        addRenderableWidget(new IntSlider(left, y, COLUMN_W, ClientConfig.MINIMAP_MIN, ClientConfig.MINIMAP_MAX,
                "gui.trailcraft.map.settings.size", ClientConfig::minimapSize, ClientConfig::setMinimapSize));
        addRenderableWidget(CycleButton.<Zoom>builder(zoom -> Component.literal(zoom.label))
                .withValues(Zoom.values()).withInitialValue(ClientConfig.minimapZoom())
                .create(right, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.zoom"),
                        (button, value) -> ClientConfig.MINIMAP_ZOOM.set(value)));

        y += ROW;
        addRenderableWidget(new IntSlider(left, y, COLUMN_W, 20, 100, "gui.trailcraft.map.settings.opacity",
                () -> Math.round(ClientConfig.minimapOpacity() * 100), ClientConfig.MINIMAP_OPACITY::set));
        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.minimapRotate())
                .create(right, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.rotate"),
                        (button, value) -> ClientConfig.MINIMAP_ROTATE.set(value)));

        y += ROW;
        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.minimapBiome())
                .create(left, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.biome"),
                        (button, value) -> ClientConfig.MINIMAP_BIOME.set(value)));
        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.minimapCoords())
                .create(right, y, COLUMN_W, 20, Component.translatable("gui.trailcraft.map.settings.coords"),
                        (button, value) -> ClientConfig.MINIMAP_COORDS.set(value)));

        y += ROW;
        addRenderableWidget(CycleButton.<DateMode>builder(mode -> Component.translatable("gui.trailcraft.map.settings.date." + mode.name().toLowerCase(Locale.ROOT)))
                .withValues(DateMode.values()).withInitialValue(ClientConfig.dateMode())
                .create(left, y, 2 * COLUMN_W + GAP, 20, Component.translatable("gui.trailcraft.map.settings.date"),
                        (button, value) -> ClientConfig.DATE_MODE.set(value)));

        y += ROW;
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.map.settings.reset"), b -> {
                    ClientConfig.resetMinimap();
                    rebuildWidgets();
                })
                .bounds(left, y, COLUMN_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.map.settings.done"), b -> onClose())
                .bounds(right, y, COLUMN_W, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Без размытия мира: миникарта в углу экрана видна чётко, и настройки меняются у неё на глазах
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

    /** Ползунок целого числа от min до max: значение пишется в настройки по ходу движения. */
    private static final class IntSlider extends AbstractSliderButton {
        private final int min, max;
        private final String key;
        private final IntSupplier get;
        private final IntConsumer set;

        IntSlider(int x, int y, int width, int min, int max, String key, IntSupplier get, IntConsumer set) {
            super(x, y, width, 20, Component.empty(), (get.getAsInt() - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.key = key;
            this.get = get;
            this.set = set;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key, get.getAsInt()));
        }

        @Override
        protected void applyValue() {
            set.accept(min + (int) Math.round(value * (max - min)));
            updateMessage();
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import io.github.ChanesCMD.trailcraft.client.map.Waypoints.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Окно путевой точки: название и цвет. Для новой точки (isNew) «Готово» добавляет её, для существующей —
 * заменяет. Enter — «Готово», Esc — отмена.
 */
public class WaypointScreen extends Screen {
    private static final int PANEL_W = 200, PANEL_H = 96, PAD = 10, SWATCH_W = 18, SWATCH_H = 12, SWATCH_GAP = 4;

    private final Screen parent;
    private final Waypoint base;
    private final boolean isNew;
    private EditBox nameBox;
    private int color;

    public WaypointScreen(Screen parent, Waypoint base, boolean isNew) {
        super(Component.translatable(isNew ? "gui.trailcraft.waypoint.new" : "gui.trailcraft.waypoint.edit"));
        this.parent = parent;
        this.base = base;
        this.isNew = isNew;
        this.color = base.color();
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return (height - PANEL_H) / 2;
    }

    private int swatchX(int i) {
        int total = Waypoints.COLORS.length * SWATCH_W + (Waypoints.COLORS.length - 1) * SWATCH_GAP;
        return panelX() + (PANEL_W - total) / 2 + i * (SWATCH_W + SWATCH_GAP);
    }

    private int swatchY() {
        return panelY() + 46;
    }

    @Override
    protected void init() {
        nameBox = new EditBox(font, panelX() + PAD, panelY() + 22, PANEL_W - 2 * PAD, 18, Component.translatable("gui.trailcraft.waypoint.name"));
        nameBox.setMaxLength(24);
        nameBox.setValue(base.name());
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);
        int half = (PANEL_W - 2 * PAD - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.waypoint.ok"), b -> confirm())
                .bounds(panelX() + PAD, panelY() + 68, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.waypoint.cancel"), b -> onClose())
                .bounds(panelX() + PAD + half + 4, panelY() + 68, half, 20).build());
    }

    private void confirm() {
        String name = nameBox.getValue().isBlank() ? base.name() : nameBox.getValue().strip();
        Waypoint result = new Waypoint(name, base.dimension(), base.x(), base.y(), base.z(), color);
        if (isNew) {
            Waypoints.add(result);
        } else {
            Waypoints.replace(base, result);
        }
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x60000000);
        UiDraw.panel(g, panelX(), panelY(), PANEL_W, PANEL_H);
        g.drawString(font, title, panelX() + PAD, panelY() + 8, UiDraw.LABEL, false);
        for (int i = 0; i < Waypoints.COLORS.length; i++) {
            int x = swatchX(i), y = swatchY();
            boolean selected = Waypoints.COLORS[i] == color;
            g.fill(x - 1, y - 1, x + SWATCH_W + 1, y + SWATCH_H + 1, selected ? 0xFFFFFFFF : 0xFF373737);
            g.fill(x, y, x + SWATCH_W, y + SWATCH_H, 0xFF000000 | Waypoints.COLORS[i]);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < Waypoints.COLORS.length; i++) {
                if (mouseX >= swatchX(i) && mouseX < swatchX(i) + SWATCH_W && mouseY >= swatchY() && mouseY < swatchY() + SWATCH_H) {
                    color = Waypoints.COLORS[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

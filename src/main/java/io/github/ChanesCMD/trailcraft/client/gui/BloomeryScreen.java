package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.BloomeryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Окно малой сыродутной печи: кувшин сверху, топливо снизу, между ними огонь, стрелка прогресса к слиткам. */
public class BloomeryScreen extends AbstractContainerScreen<BloomeryMenu> {
    private static final int FLAME = 0xFFE8701A, FLAME_CORE = 0xFFFFC23A;

    public BloomeryScreen(BloomeryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BloomeryMenu.WIDTH;
        imageHeight = BloomeryMenu.HEIGHT;
        inventoryLabelY = BloomeryMenu.HOTBAR_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiDraw.panel(g, x, y, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            UiDraw.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        // огонь между кувшином и топливом: 14×14, убывает сверху вниз
        int fx = x + BloomeryMenu.FUEL_X + 1, fy = y + BloomeryMenu.JUG_Y + 20;
        g.fill(fx, fy, fx + 14, fy + 14, 0xFF8B8B8B);
        int h = Math.round(14 * menu.burn());
        if (h > 0) {
            g.fill(fx, fy + 14 - h, fx + 14, fy + 14, FLAME);
            g.fill(fx + 4, fy + 14 - Math.max(1, h * 2 / 3), fx + 10, fy + 14, FLAME_CORE);
        }
        // стрелка прогресса к слиткам
        int ax = x + BloomeryMenu.JUG_X + 26, ay = y + BloomeryMenu.OUT_Y + 10;
        UiDraw.arrow(g, ax, ay);
        int w = Math.round(22 * menu.progress());
        if (w > 0) {
            g.enableScissor(ax, ay, ax + w, ay + 15);
            g.fill(ax, ay + 5, ax + 14, ay + 10, FLAME);
            for (int i = 0; i < 8; i++) {
                g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + 15 - i, FLAME);
            }
            g.disableScissor();
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.LargeBloomeryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Окно большого сыродутного горна. */
public class LargeBloomeryScreen extends AbstractContainerScreen<LargeBloomeryMenu> {
    private static final int FLAME = 0xFFE8701A, FLAME_CORE = 0xFFFFC23A;

    public LargeBloomeryScreen(LargeBloomeryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LargeBloomeryMenu.WIDTH;
        imageHeight = LargeBloomeryMenu.HEIGHT;
        inventoryLabelY = LargeBloomeryMenu.HOTBAR_Y - 11;
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
        int fx = x + LargeBloomeryMenu.ORE_X + 1, fy = y + LargeBloomeryMenu.ORE_Y + 20;
        g.fill(fx, fy, fx + 14, fy + 14, 0xFF8B8B8B);
        int h = Math.round(14 * menu.progress());
        if (h > 0) {
            g.fill(fx, fy + 14 - h, fx + 14, fy + 14, FLAME);
            g.fill(fx + 4, fy + 14 - Math.max(1, h * 2 / 3), fx + 10, fy + 14, FLAME_CORE);
        }
        int ax = x + LargeBloomeryMenu.ORE_X + 22, ay = y + LargeBloomeryMenu.OUT_Y + 5;
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

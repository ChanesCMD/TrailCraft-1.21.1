package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.BloomeryMenu;
import io.github.ChanesCMD.trailcraft.smelting.JugItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Окно малой сыродутной печи: кувшин сверху, топливо снизу, между ними огонь. Выхода у печи нет — справа полоска
 * нагрева кувшина с меткой 80 %, с которой металл становится жидким и его можно лить в формы.
 */
public class BloomeryScreen extends AbstractContainerScreen<BloomeryMenu> {
    private static final int FLAME = 0xFFE8701A, FLAME_CORE = 0xFFFFC23A;
    private static final float POUR_MARK = JugItem.MELT_HEAT / (float) JugItem.MAX_HEAT;

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
        // нагрев кувшина
        UiDraw.heatBar(g, x + BloomeryMenu.HEAT_X, y + BloomeryMenu.HEAT_Y,
                BloomeryMenu.HEAT_W, BloomeryMenu.HEAT_H, menu.heat(), POUR_MARK);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int percent = Math.round(menu.heat() * JugItem.MAX_HEAT);
        g.drawString(font, Component.translatable("gui.trailcraft.heat", percent),
                BloomeryMenu.HEAT_X, BloomeryMenu.HEAT_Y + BloomeryMenu.HEAT_H + 4, UiDraw.LABEL, false);
        if (menu.canPour()) {
            g.drawString(font, Component.translatable("gui.trailcraft.heat.pour"),
                    BloomeryMenu.HEAT_X, BloomeryMenu.HEAT_Y + BloomeryMenu.HEAT_H + 15, 0xFFB04A00, false);
        }
    }
}

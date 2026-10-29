package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.bee.ApiaryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Окно пасеки: две пчелы, между ними и выходом — стрелка цикла (медовая), под пчёлами — почему пасека стоит. */
public class ApiaryScreen extends AbstractContainerScreen<ApiaryMenu> {
    private static final int HONEY = 0xFFF2A21C;
    private static final String[] STATUS = {"ok", "no_bees", "night", "rain", "no_sky", "full"};

    public ApiaryScreen(ApiaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ApiaryMenu.WIDTH;
        imageHeight = ApiaryMenu.HEIGHT;
        inventoryLabelY = ApiaryMenu.HOTBAR_Y - 11;
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
        int ax = x + ApiaryMenu.BEE_X + 34, ay = y + ApiaryMenu.OUTPUT_Y + 19;
        UiDraw.arrow(g, ax, ay);
        int w = Math.round(22 * menu.progress());
        if (w > 0) {
            g.enableScissor(ax, ay, ax + w, ay + 15);
            g.fill(ax, ay + 5, ax + 14, ay + 10, HONEY);
            for (int i = 0; i < 8; i++) {
                g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + 15 - i, HONEY);
            }
            g.disableScissor();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int status = menu.status();
        Component text = Component.translatable("gui.trailcraft.apiary." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]);
        g.pose().pushPose();
        g.pose().translate(8, ApiaryMenu.BEE_B_Y + 22, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, text, 0, 0, status == 0 ? 0xFF3F8F2F : 0xFFB04A00, false);
        g.pose().popPose();
    }
}

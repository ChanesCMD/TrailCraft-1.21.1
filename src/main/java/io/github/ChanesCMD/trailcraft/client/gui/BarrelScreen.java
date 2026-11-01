package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.inventory.BarrelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Окно бочки в духе TFC: слева высокий бак с жидкостью и делениями, справа шкуры и кожа, между ними — вертикальный
 * прогресс дубления, рядом кнопка запечатывания (замок). Пока бочка запечатана, слоты затемнены и заперты, а дубление
 * идёт; открытая бочка прогресс хранит. Внизу — хотбар.
 */
public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {
    private static final int[] COLORS = {0, 0xFF4E81E6, 0xFF5E8CE8, 0xFF7A4A22};
    private static final int[] LIGHT = {0, 0xFF7FA6F0, 0xFF86ACF2, 0xFF9A6234};
    private static final String[] KINDS = {"", "block.trailcraft.water_barrel.fresh", "block.trailcraft.water_barrel.salty",
            "fluid_type.trailcraft.tannin"};
    private static final int TANNIN_COLOR = 0xFFB0702E;

    public BarrelScreen(BarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BarrelMenu.WIDTH;
        imageHeight = BarrelMenu.HEIGHT;
        inventoryLabelY = BarrelMenu.HOTBAR_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (menu.sealed()) {
            drawLock(g, BarrelMenu.HIDES_X, BarrelMenu.HIDES_Y);
            drawLock(g, BarrelMenu.LEATHER_X, BarrelMenu.LEATHER_Y);
        }
        renderTooltip(g, mouseX, mouseY);
        if (isOver(mouseX, mouseY, BarrelMenu.TANK_X, BarrelMenu.TANK_Y, BarrelMenu.TANK_W, BarrelMenu.TANK_H)) {
            int kind = menu.fluidKind();
            Component what = kind == WaterBarrelBlockEntity.EMPTY
                    ? Component.translatable("gui.trailcraft.barrel.empty")
                    : Component.translatable(KINDS[kind]);
            g.renderComponentTooltip(font, List.of(what,
                    Component.translatable("gui.trailcraft.barrel.amount", menu.amount(), WaterBarrelBlockEntity.CAPACITY)),
                    mouseX, mouseY);
        }
        if (isOver(mouseX, mouseY, BarrelMenu.SEAL_X, BarrelMenu.SEAL_Y, BarrelMenu.SEAL_SIZE, BarrelMenu.SEAL_SIZE)) {
            g.renderTooltip(font, Component.translatable(menu.sealed()
                    ? "gui.trailcraft.barrel.unseal" : "gui.trailcraft.barrel.seal"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOver(mouseX, mouseY, BarrelMenu.SEAL_X, BarrelMenu.SEAL_Y,
                BarrelMenu.SEAL_SIZE, BarrelMenu.SEAL_SIZE)) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiDraw.panel(g, x, y, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            UiDraw.slot(g, x + slot.x - 1, y + slot.y - 1);
        }

        // бак: рамка, столбик жидкости снизу вверх, деления через 2000 mB и отметка 4000 mB (сколько нужно для дубления)
        int tx = x + BarrelMenu.TANK_X, ty = y + BarrelMenu.TANK_Y;
        int w = BarrelMenu.TANK_W, h = BarrelMenu.TANK_H;
        UiDraw.frame(g, tx - 1, ty - 1, w + 2, h + 2, 0xFF373737, 0xFFFFFFFF, 0xFF8B8B8B);
        int kind = menu.fluidKind();
        int filled = Math.round((float) h * menu.amount() / WaterBarrelBlockEntity.CAPACITY);
        if (kind != WaterBarrelBlockEntity.EMPTY && filled > 0) {
            g.fill(tx, ty + h - filled, tx + w, ty + h, COLORS[kind]);
            g.fill(tx, ty + h - filled, tx + w, ty + h - filled + 1, LIGHT[kind]);
        }
        for (int mb = 2000; mb < WaterBarrelBlockEntity.CAPACITY; mb += 2000) {
            int yy = ty + h - Math.round((float) h * mb / WaterBarrelBlockEntity.CAPACITY);
            g.fill(tx, yy, tx + 4, yy + 1, 0x66000000);
        }
        int mark = ty + h - Math.round((float) h * WaterBarrelBlockEntity.TANNIN_NEEDED / WaterBarrelBlockEntity.CAPACITY);
        g.fill(tx, mark, tx + w, mark + 1, 0xFF373737);

        // вертикальный прогресс дубления между шкурами и кожей
        int ax = x + BarrelMenu.HIDES_X + 6, ay = y + BarrelMenu.HIDES_Y + 18;
        int shaft = BarrelMenu.LEATHER_Y - (BarrelMenu.HIDES_Y + 18) - 5;
        g.fill(ax, ay, ax + 5, ay + shaft, 0xFF8B8B8B);
        int ph = Math.round(shaft * Math.min(1f, menu.soak()));
        if (ph > 0) {
            g.fill(ax, ay + shaft - ph, ax + 5, ay + shaft, TANNIN_COLOR);
        }
        for (int i = 0; i < 5; i++) {
            g.fill(ax - i, ay + shaft + i, ax + 5 + i, ay + shaft + i + 1, 0xFF8B8B8B);
        }
        if (ph > 0) {
            for (int i = 0; i < 5; i++) {
                g.fill(ax - i, ay + shaft + i, ax + 5 + i, ay + shaft + i + 1, TANNIN_COLOR);
            }
        }

        drawSealButton(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        label(g, "gui.trailcraft.barrel.hides", BarrelMenu.HIDES_X + 22, BarrelMenu.HIDES_Y + 4);
        label(g, "gui.trailcraft.barrel.leather", BarrelMenu.LEATHER_X + 22, BarrelMenu.LEATHER_Y + 4);

        Component status = status();
        g.pose().pushPose();
        g.pose().translate(imageWidth / 2f, 76, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, status, -font.width(status) / 2, 0, statusColor(), false);
        g.pose().popPose();
    }

    private void label(GuiGraphics g, String key, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.7f, 0.7f, 1);
        g.drawString(font, Component.translatable(key), 0, 0, UiDraw.LABEL, false);
        g.pose().popPose();
    }

    private Component status() {
        if (!menu.sealed()) {
            return Component.translatable("gui.trailcraft.barrel.unsealed");
        }
        if (!menu.hasHides()) {
            return Component.translatable("gui.trailcraft.barrel.sealed");
        }
        if (menu.leather() + menu.hides() > 64) {
            return Component.translatable("gui.trailcraft.barrel.no_space");
        }
        if (menu.fluidKind() != WaterBarrelBlockEntity.TANNIN
                || menu.amount() < WaterBarrelBlockEntity.TANNIN_NEEDED) {
            return Component.translatable("gui.trailcraft.barrel.need_tannin", WaterBarrelBlockEntity.TANNIN_NEEDED);
        }
        return Component.translatable("gui.trailcraft.barrel.tanning", Math.round(menu.soak() * 100));
    }

    private int statusColor() {
        if (!menu.sealed() || !menu.hasHides()) {
            return 0xFF6E6E6E;
        }
        if (menu.leather() + menu.hides() > 64 || menu.fluidKind() != WaterBarrelBlockEntity.TANNIN
                || menu.amount() < WaterBarrelBlockEntity.TANNIN_NEEDED) {
            return 0xFFB04A00;
        }
        return 0xFF3F8F2F;
    }

    private void drawSealButton(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + BarrelMenu.SEAL_X, y = topPos + BarrelMenu.SEAL_Y;
        boolean hover = mouseX >= x && mouseX < x + BarrelMenu.SEAL_SIZE
                && mouseY >= y && mouseY < y + BarrelMenu.SEAL_SIZE;
        UiDraw.frame(g, x, y, BarrelMenu.SEAL_SIZE, BarrelMenu.SEAL_SIZE, 0xFF373737, 0xFFFFFFFF,
                hover ? 0xFFD8D8D8 : UiDraw.PANEL);
        UiDraw.padlock(g, x + 6, y + 5, !menu.sealed(), 0xFF4A3218);
    }

    private void drawLock(GuiGraphics g, int sx, int sy) {
        int x = leftPos + sx - 1, y = topPos + sy - 1;
        g.fill(x, y, x + 18, y + 18, 0x77000000);
        UiDraw.padlock(g, x + 5, y + 4, false, 0xFFFFFFFF);
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }
}

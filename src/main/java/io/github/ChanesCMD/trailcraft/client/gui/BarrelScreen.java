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
 * Окно бочки: бак (полоса цвета жидкости, подсказка — сколько и чего), выскобленные шкуры → стрелка дубления → кожа.
 * Если шкуры лежат, а танина меньше нужного, под стрелкой — сколько его надо.
 */
public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {
    private static final int[] COLORS = {0, 0xFF4E81E6, 0xFF5E8CE8, 0xFF7A4A22};
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
        renderTooltip(g, mouseX, mouseY);
        int tx = leftPos + BarrelMenu.TANK_X, ty = topPos + BarrelMenu.TANK_Y;
        if (mouseX >= tx && mouseX < tx + BarrelMenu.TANK_W && mouseY >= ty && mouseY < ty + BarrelMenu.TANK_H) {
            int kind = menu.fluidKind();
            Component what = kind == WaterBarrelBlockEntity.EMPTY ? Component.translatable("gui.trailcraft.barrel.empty")
                    : Component.translatable(KINDS[kind]);
            g.renderComponentTooltip(font, List.of(what, Component.translatable("gui.trailcraft.barrel.amount",
                    menu.amount(), WaterBarrelBlockEntity.CAPACITY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiDraw.panel(g, x, y, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            UiDraw.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        // бак: рамка и столбик жидкости снизу вверх
        int tx = x + BarrelMenu.TANK_X, ty = y + BarrelMenu.TANK_Y, w = BarrelMenu.TANK_W, h = BarrelMenu.TANK_H;
        UiDraw.frame(g, tx - 1, ty - 1, w + 2, h + 2, 0xFF373737, 0xFFFFFFFF, 0xFF8B8B8B);
        int kind = menu.fluidKind();
        int filled = Math.round((float) h * menu.amount() / WaterBarrelBlockEntity.CAPACITY);
        if (kind != WaterBarrelBlockEntity.EMPTY && filled > 0) {
            g.fill(tx, ty + h - filled, tx + w, ty + h, COLORS[kind]);
        }
        // отметка «сколько танина нужно для дубления»
        int mark = ty + h - Math.round((float) h * WaterBarrelBlockEntity.TANNIN_NEEDED / WaterBarrelBlockEntity.CAPACITY);
        g.fill(tx, mark, tx + w, mark + 1, 0xFF373737);
        // стрелка дубления
        int ax = x + BarrelMenu.HIDES_X + 25, ay = y + BarrelMenu.SLOT_Y;
        UiDraw.arrow(g, ax, ay);
        int p = Math.round(22 * menu.soak());
        if (p > 0) {
            g.enableScissor(ax, ay, ax + p, ay + 15);
            g.fill(ax, ay + 5, ax + 14, ay + 10, TANNIN_COLOR);
            for (int i = 0; i < 8; i++) {
                g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + 15 - i, TANNIN_COLOR);
            }
            g.disableScissor();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (menu.hasHides() && (menu.fluidKind() != WaterBarrelBlockEntity.TANNIN
                || menu.amount() < WaterBarrelBlockEntity.TANNIN_NEEDED)) {
            g.pose().pushPose();
            g.pose().translate(BarrelMenu.HIDES_X, BarrelMenu.SLOT_Y + 22, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, Component.translatable("gui.trailcraft.barrel.need_tannin", WaterBarrelBlockEntity.TANNIN_NEEDED),
                    0, 0, 0xFFB04A00, false);
            g.pose().popPose();
        }
    }
}

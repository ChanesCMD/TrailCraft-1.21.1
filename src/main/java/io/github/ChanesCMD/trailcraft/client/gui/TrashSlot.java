package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import io.github.ChanesCMD.trailcraft.inventory.TrashPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Мусорка справа от хотбара — в инвентаре TrailCraft и в креативе (окна одного размера, хотбар на одном месте):
 * клик с предметом на курсоре — предмет исчезает. В выживании убирает сервер (TrashPayload), в креативе курсор
 * только на клиенте, как у ванильной мусорки креатива.
 */
public final class TrashSlot {
    /** Рамка слота относительно окна: под первым столбцом сетки крафта, в ряд с хотбаром. */
    public static final int X = TrailInventoryMenu.CRAFT_X, Y = TrailInventoryMenu.HOTBAR_Y;

    private TrashSlot() {}

    public static boolean isOver(int left, int top, double mouseX, double mouseY) {
        return mouseX >= left + X && mouseX < left + X + 18 && mouseY >= top + Y && mouseY < top + Y + 18;
    }

    /** Слот и значок урны (крышка с ручкой, корпус с прорезями); под курсором — подсветка. */
    public static void render(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        int x = left + X, y = top + Y;
        UiDraw.slot(g, x, y);
        int dark = 0xFF4A4A4A, mid = 0xFF6E6E6E;
        g.fill(x + 7, y + 3, x + 11, y + 4, dark);      // ручка
        g.fill(x + 4, y + 4, x + 14, y + 6, dark);      // крышка
        g.fill(x + 5, y + 6, x + 13, y + 15, dark);     // корпус
        g.fill(x + 6, y + 7, x + 12, y + 14, mid);
        for (int i = 0; i < 3; i++) {
            g.fill(x + 7 + 2 * i, y + 8, x + 8 + 2 * i, y + 13, dark);
        }
        if (isOver(left, top, mouseX, mouseY)) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
        }
    }

    public static void renderTooltip(GuiGraphics g, Font font, int left, int top, int mouseX, int mouseY, AbstractContainerMenu menu) {
        if (isOver(left, top, mouseX, mouseY) && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.trailcraft.trash"), mouseX, mouseY);
        }
    }

    /** Клик по мусорке: true — клик обработан (предмет с курсора удалён или курсор пуст). */
    public static boolean click(int left, int top, double mouseX, double mouseY, AbstractContainerMenu menu, boolean creative) {
        if (!isOver(left, top, mouseX, mouseY)) {
            return false;
        }
        if (!menu.getCarried().isEmpty()) {
            if (!creative) {
                PacketDistributor.sendToServer(TrashPayload.INSTANCE);
            }
            menu.setCarried(ItemStack.EMPTY);
        }
        return true;
    }
}

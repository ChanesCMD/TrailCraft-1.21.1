package io.github.ChanesCMD.trailcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Рисование в ванильном стиле без текстур: панели, рамки слотов, кружки-кнопки. */
public final class UiDraw {
    public static final int PANEL = 0xFFC6C6C6;
    public static final int LABEL = 0xFF404040;
    public static final int AMBER = 0xFFF2B233;
    public static final int GREEN = 0xFF55D655;
    public static final int GREY = 0xFF8B8B8B;

    /** Панель: чёрный контур, белый блик сверху-слева, #555555 снизу-справа. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    /** Рамка слота 18×18, как в ванилле: тёмная сверху-слева, белая снизу-справа. */
    public static void slot(GuiGraphics g, int x, int y) {
        frame(g, x, y, 18, 18, 0xFF373737, 0xFFFFFFFF, GREY);
    }

    /** Заблокированный слот: темнее, с пиксельным замком в центре. */
    public static void lockedSlot(GuiGraphics g, int x, int y) {
        frame(g, x, y, 18, 18, 0xFF2E2E2E, 0xFF9E9E9E, 0xFF6E6E6E);
        int lock = 0xFF3A3A3A;
        g.fill(x + 7, y + 4, x + 11, y + 5, lock);   // дужка
        g.fill(x + 6, y + 5, x + 7, y + 8, lock);
        g.fill(x + 11, y + 5, x + 12, y + 8, lock);
        g.fill(x + 5, y + 8, x + 13, y + 14, lock);  // корпус
        g.fill(x + 8, y + 10, x + 10, y + 12, 0xFF6E6E6E); // скважина
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int dark, int light, int fill) {
        g.fill(x, y, x + w, y + h, dark);
        g.fill(x + 1, y + 1, x + w, y + h, light);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** Пиксельный круг 5×5 или 7×7. */
    public static void circle(GuiGraphics g, int x, int y, int size, int color) {
        int[] widths = size == 5 ? new int[]{3, 5, 5, 5, 3} : new int[]{3, 5, 7, 7, 7, 5, 3};
        for (int row = 0; row < widths.length; row++) {
            int inset = (size - widths[row]) / 2;
            g.fill(x + inset, y + row, x + inset + widths[row], y + row + 1, color);
        }
    }

    /** Круг 5×5 или 7×7 с обводкой в 1 px. */
    public static void outlinedCircle(GuiGraphics g, int x, int y, int size, int fill, int outline) {
        circle(g, x, y, size, outline);
        if (size == 7) {
            circle(g, x + 1, y + 1, 5, fill);
        } else {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
        }
    }

    /** Янтарная кнопка окна 7×7 с белым «+». */
    public static void plusButton(GuiGraphics g, int x, int y) {
        circle(g, x, y, 7, AMBER);
        g.fill(x + 3, y + 2, x + 4, y + 5, 0xFFFFFFFF);
        g.fill(x + 2, y + 3, x + 5, y + 4, 0xFFFFFFFF);
    }

    /** Серая стрелка крафта 22×15. */
    public static void arrow(GuiGraphics g, int x, int y) {
        g.fill(x, y + 5, x + 14, y + 10, GREY);
        for (int i = 0; i < 8; i++) {
            g.fill(x + 14 + i, y + i, x + 15 + i, y + 15 - i, GREY);
        }
    }

    private UiDraw() {}
}

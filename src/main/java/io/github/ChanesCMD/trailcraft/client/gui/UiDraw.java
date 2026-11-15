package io.github.ChanesCMD.trailcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

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

    /** Синяя кнопка окна 7×7 с тёмно-синей обводкой и белым «+». */
    public static void plusButton(GuiGraphics g, int x, int y) {
        outlinedCircle(g, x, y, 7, 0xFF4A7BE0, 0xFF1E3A78);
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

    /** Полоска нагрева: холодная — синевато-серая, горячая — оранжевая; белая метка — порог литья (mark долей). */
    public static void heatBar(GuiGraphics g, int x, int y, int w, int h, float value, float mark) {
        frame(g, x, y, w, h, 0xFF373737, 0xFFFFFFFF, 0xFF262626);
        int room = Math.max(0, w - 2);
        int fill = Math.round(room * Mth.clamp(value, 0f, 1f));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, heatColor(value));
        }
        if (mark > 0f && mark < 1f) {
            int mx = x + 1 + Math.round(room * mark);
            g.fill(mx, y + 1, mx + 1, y + h - 1, 0xFFFFFFFF);
        }
    }

    /** Та же полоска, но растёт снизу вверх. */
    public static void heatBarVertical(GuiGraphics g, int x, int y, int w, int h, float value, float mark) {
        frame(g, x, y, w, h, 0xFF373737, 0xFFFFFFFF, 0xFF262626);
        int room = Math.max(0, h - 2);
        int fill = Math.round(room * Mth.clamp(value, 0f, 1f));
        if (fill > 0) {
            g.fill(x + 1, y + h - 1 - fill, x + w - 1, y + h - 1, heatColor(value));
        }
        if (mark > 0f && mark < 1f) {
            int my = y + h - 1 - Math.round(room * mark);
            g.fill(x + 1, my, x + w - 1, my + 1, 0xFFFFFFFF);
        }
    }

    /** Цвет нагрева: синевато-серый → оранжевый → жёлто-белый. */
    public static int heatColor(float value) {
        float v = Mth.clamp(value, 0f, 1f);
        return v < 0.5f ? mix(0xFF6E7C8C, 0xFFE8701A, v / 0.5f)
                : mix(0xFFE8701A, 0xFFFFE98A, (v - 0.5f) / 0.5f);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + ((((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t));
        int gr = Math.round(((a >> 8) & 0xFF) + ((((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t));
        int bl = Math.round((a & 0xFF) + (((b & 0xFF) - (a & 0xFF)) * t));
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    /** Пиксельный замок 8×10: у открытого дужка сдвинута вправо, у закрытого — по центру. */
    public static void padlock(GuiGraphics g, int x, int y, boolean open, int color) {
        int shift = open ? 2 : 0;
        g.fill(x + 2 + shift, y, x + 6 + shift, y + 1, color);
        g.fill(x + 1 + shift, y + 1, x + 2 + shift, y + 4, color);
        g.fill(x + 6 + shift, y + 1, x + 7 + shift, y + 4, color);
        g.fill(x, y + 4, x + 8, y + 10, color);
        g.fill(x + 3, y + 6, x + 5, y + 8, 0xFF555555);
        g.fill(x + 3, y + 8, x + 4, y + 9, 0xFF555555);
    }

    private UiDraw() {}
}

package io.github.ChanesCMD.trailcraft.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Значки HUD 9×9, нарисованные пикселями (заглушки до своих текстур). В шаблоне: 'o' — контур, 'f' — заливка,
 * 'h' — блик, 'l' — ботва, '.' — пусто. Палитра — {контур, заливка, блик[, ботва]}; без цвета ботвы она как заливка.
 */
public final class HudIcons {
    public static final String[] DROP = {
            "....o....",
            "...ofo...",
            "...ofo...",
            "..offfo..",
            ".ofhfffo.",
            ".ohffffo.",
            ".offfffo.",
            "..offfo..",
            "...ooo..."};
    /** Морковь остриём вниз-влево, 'l' — ботва. */
    public static final String[] FOOD = {
            "......l.l",
            ".....ooll",
            "....ohfo.",
            "...ohffo.",
            "..ohffo..",
            "..offo...",
            ".offo....",
            ".oo......",
            "o........"};
    public static final String[] BUBBLE = {
            "..ooooo..",
            ".ohhfffo.",
            "ohhfffffo",
            "ohffffffo",
            "offfffffo",
            "offfffffo",
            "offfffffo",
            ".offfffo.",
            "..ooooo.."};
    public static final String[] BOLT = {
            "....oooo.",
            "...ohfo..",
            "..offo...",
            ".offoooo.",
            ".offfffo.",
            ".oooffo..",
            "...offo..",
            "..offo...",
            "..ooo...."};

    public static final int[] WATER = {0xFF10204A, 0xFF3F76E4, 0xFF8FB8FF};
    /** Капли при «Солёной жажде» — серо-зелёные, как морковки при «Голоде». */
    public static final int[] SALT_WATER = {0xFF1E3A2E, 0xFF6E9A86, 0xFFC4DCD0};
    public static final int[] FOOD_COLORS = {0xFF5A2A06, 0xFFE8761E, 0xFFFFB061, 0xFF3E9A2B};
    public static final int[] HUNGER_COLORS = {0xFF22330A, 0xFF7A9A2B, 0xFFB5D25C, 0xFF4A5A1A};
    public static final int[] AIR = {0xFF2A5A8A, 0xFF9FD3FF, 0xFFFFFFFF};
    public static final int[] STAMINA = {0xFF4A3A00, 0xFFF2D23A, 0xFFFFF59A};
    /** Пустое деление — тёмно-серое. */
    public static final int[] EMPTY = {0xFF141414, 0xFF3A3A3A, 0xFF3A3A3A};

    /** Значок: full — сколько половинок закрашено (0 — пустой, 1 — половина, 2 — целый). */
    public static void draw(GuiGraphics g, int x, int y, String[] pattern, int[] colors, int full) {
        draw(g, x, y, pattern, colors, full, false);
    }

    /** То же; rightHalf — половинка справа (для рядов, которые заполняются справа налево). */
    public static void draw(GuiGraphics g, int x, int y, String[] pattern, int[] colors, int full, boolean rightHalf) {
        for (int row = 0; row < 9; row++) {
            String line = pattern[row];
            for (int col = 0; col < 9; col++) {
                char c = line.charAt(col);
                if (c == '.') {
                    continue;
                }
                boolean filled = full == 2 || full == 1 && (rightHalf ? col >= 4 : col < 5);
                int[] palette = filled ? colors : EMPTY;
                int color = c == 'o' ? palette[0] : c == 'h' ? palette[2] : c == 'l' && palette.length > 3 ? palette[3] : palette[1];
                g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
            }
        }
    }

    /** Только контур значка одним цветом (насыщение поверх морковок); full — сколько половинок, как в draw. */
    public static void drawOutline(GuiGraphics g, int x, int y, String[] pattern, int color, int full) {
        for (int row = 0; row < 9; row++) {
            String line = pattern[row];
            for (int col = 0; col < 9; col++) {
                if (line.charAt(col) == 'o' && (full == 2 || full == 1 && col < 5)) {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    /** Палитра с другой прозрачностью (alpha 0..255). */
    public static int[] withAlpha(int[] colors, int alpha) {
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            out[i] = alpha << 24 | colors[i] & 0xFFFFFF;
        }
        return out;
    }

    private HudIcons() {}
}

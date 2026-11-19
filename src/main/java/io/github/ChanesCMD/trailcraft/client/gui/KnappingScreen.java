package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.FlintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Миниигра оббивки кремня: на плите лежит кусок кремня, поверх — контур головки.
 * Полоска откола идёт за курсором и режет кремень ровно по себе: всё, что осталось по ту сторону
 * маленькой стрелки, откалывается. Отколол лишнее — кремень испорчен; оставил много лишнего — игра не заканчивается;
 * срезал почти всё, но не идеально — головка плохого качества.
 */
public class KnappingScreen extends Screen {
    private static final int GRID = 32;         // клеток на плите (вчетверо больше прежнего)
    private static final int CELL = 6;
    private static final int BOARD = GRID * CELL;
    private static final int MAX_BLUNDER = 10;

    private static final String[][] SHAPES = {
            {"................",
             "......######....",
             "....##......##..",
             "...##........##.",
             "..##..........##",
             "..##..........##",
             "..##..........##",
             "...##........##.",
             "....##......##..",
             "......######....",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................"},
            {"................",
             ".....#####......",
             "....#######.....",
             "...#########....",
             "...#########....",
             "...#########....",
             "...########.....",
             "....#######.....",
             ".....#####......",
             "......###.......",
             ".......#........",
             "................",
             "................",
             "................",
             "................",
             "................"},
            {"................",
             "......####......",
             "......####......",
             "......####......",
             "......####......",
             "......####......",
             "......####......",
             "......####......",
             "......####......",
             ".......##.......",
             ".......##.......",
             "................",
             "................",
             "................",
             "................",
             "................"},
            {"................",
             "...##########...",
             "...##......##...",
             "...........##...",
             "...........##...",
             "...........##...",
             "...........##...",
             "...........##...",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................"},
            {"................",
             "....##..........",
             "...####.........",
             "...#####........",
             "...######.......",
             "....######......",
             "....######......",
             ".....#####......",
             ".....####.......",
             "......##........",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................"},
            {"................",
             ".......##.......",
             "......####......",
             "......####......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             ".......##.......",
             "................",
             "................",
             "................",
             "................"},
    };

    private final int tool;
    private final boolean[][] stone = new boolean[GRID][GRID];
    private final boolean[][] shape = new boolean[GRID][GRID];
    private int blunder;
    private boolean finished;
    private boolean poor;
    private int flash;
    private boolean sent;
    /** Курсор на плите в клетках. */
    private double aimX = GRID / 2.0, aimY = -2;
    /** Угол полоски в градусах: Q/E. */
    private float angle = 90f;
    /** Малое обтачивание: полоска короткая (около 3 клеток) и шаг поворота вдвое меньше. */
    private boolean small;

    public KnappingScreen(int tool) {
        super(Component.translatable("gui.trailcraft.knapping"));
        this.tool = tool;
        String[] art = SHAPES[Math.min(tool, SHAPES.length - 1)];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                shape[y][x] = art[y / 2].charAt(x / 2) == '#';
            }
        }
        double c = (GRID - 1) / 2.0;
        double r = GRID / 2.0 - 0.5;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                double dx = (x - c) / r, dy = (y - c) / r;
                stone[y][x] = dx * dx + dy * dy <= 0.92;
            }
        }
    }

    public static void open(int tool) {
        Minecraft.getInstance().setScreen(new KnappingScreen(tool));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (width - BOARD) / 2 - 60;
    }

    private int top() {
        return (height - BOARD) / 2 - 10;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = left(), y0 = top();
        aimX = (mouseX - x0) / (double) CELL;
        aimY = (mouseY - y0) / (double) CELL;
        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);   // сторона, которая откалывается
        double tx = -ny, ty = nx;                        // вдоль полоски

        UiDraw.panel(g, x0 - 16, y0 - 30, BOARD + 32, BOARD + 84);
        g.drawString(font, title, x0 - 6, y0 - 23, UiDraw.LABEL, false);
        g.drawString(font, Component.translatable("gui.trailcraft.knapping.head").append(FlintItem.head(tool).getHoverName()),
                x0 - 6, y0 - 12, UiDraw.GREY, false);

        g.fill(x0, y0, x0 + BOARD, y0 + BOARD, 0xFF2B2B2B);
        UiDraw.frame(g, x0 - 1, y0 - 1, BOARD + 2, BOARD + 2, 0xFF1A1A1A, 0xFF4A4A4A, 0xFF1A1A1A);

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int px = x0 + x * CELL;
                int py = y0 + y * CELL;
                if (stone[y][x]) {
                    int shade = ((x * 31 + y * 17) % 7) == 0 ? 0xFF8A8A91 : 0xFF9A9AA0;
                    g.fill(px, py, px + CELL, py + CELL, shade);
                } else if (shape[y][x]) {
                    g.fill(px, py, px + CELL, py + CELL, 0x28C8B080);
                }
            }
        }
        // контур головки
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!shape[y][x]) {
                    continue;
                }
                int px = x0 + x * CELL, py = y0 + y * CELL;
                if (y == 0 || !shape[y - 1][x]) {
                    g.fill(px, py, px + CELL, py + 1, 0xFFD8A45A);
                }
                if (y == GRID - 1 || !shape[y + 1][x]) {
                    g.fill(px, py + CELL - 1, px + CELL, py + CELL, 0xFFD8A45A);
                }
                if (x == 0 || !shape[y][x - 1]) {
                    g.fill(px, py, px + 1, py + CELL, 0xFFD8A45A);
                }
                if (x == GRID - 1 || !shape[y][x + 1]) {
                    g.fill(px + CELL - 1, py, px + CELL, py + CELL, 0xFFD8A45A);
                }
            }
        }
        // полоска откола через курсор, поперёк направления; в малом режиме она короткая
        small = hasShiftDown();
        double half = (small ? 1.6 : BOARD / (double) CELL) * CELL;
        double mx = x0 + aimX * CELL, my = y0 + aimY * CELL;
        int lx = (int) Math.round(mx + tx * half), ly = (int) Math.round(my + ty * half);
        int rx = (int) Math.round(mx - tx * half), ry = (int) Math.round(my - ty * half);
        drawLine(g, lx, ly, rx, ry, flash > 0 ? 0xFFFFE08A : 0xFFF2F2F2);
        // маленькая стрелка снаружи: в эту сторону откалывается
        int ax = (int) Math.round(mx + nx * 9), ay = (int) Math.round(my + ny * 9);
        int bx = (int) Math.round(mx + nx * 17), by = (int) Math.round(my + ny * 17);
        drawLine(g, ax, ay, bx, by, 0xFFE8C070);
        drawLine(g, bx, by, bx - (int) Math.round(nx * 4 - tx * 3), by - (int) Math.round(ny * 4 - ty * 3), 0xFFE8C070);
        drawLine(g, bx, by, bx - (int) Math.round(nx * 4 + tx * 3), by - (int) Math.round(ny * 4 + ty * 3), 0xFFE8C070);

        g.drawString(font, Component.translatable("gui.trailcraft.knapping.angle", Math.round((angle % 360 + 360) % 360))
                        .append("  ").append(Component.translatable(small
                                ? "gui.trailcraft.knapping.small" : "gui.trailcraft.knapping.full")),
                x0 - 6, y0 + BOARD + 4, small ? 0xFFD8A45A : UiDraw.GREY, false);
        g.drawString(font, Component.translatable("gui.trailcraft.knapping.keys"),
                x0 - 6, y0 + BOARD + 14, 0xFF707070, false);
        if (finished) {
            Component text = Component.translatable(poor
                    ? "gui.trailcraft.knapping.poor" : "gui.trailcraft.knapping.good");
            g.drawString(font, text, x0 - 6, y0 + BOARD + 20, poor ? 0xFFD8A45A : 0xFF7BC67B, false);
        } else if (blunder > MAX_BLUNDER / 2) {
            g.drawString(font, Component.translatable("gui.trailcraft.knapping.cracking"),
                    x0 - 6, y0 + BOARD + 20, 0xFFC06050, false);
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1, y = y1;
        for (int i = 0; i < 2000; i++) {
            g.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) {
                return;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        float step = small ? 3.75f : 7.5f;
        if (keyCode == 81) {                 // Q
            angle = (angle - step + 360f) % 360f;
            return true;
        }
        if (keyCode == 69) {                 // E
            angle = (angle + step) % 360f;
            return true;
        }
        if (keyCode == 32) {
            if (finished) {
                sendResult();
                return true;
            }
            chip();
            return true;
        }
        if (keyCode == 256 && finished) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (finished) {
            sendResult();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Отколоть всё, что осталось по ту сторону полоски (за стрелкой). */
    private void chip() {
        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);
        double line = (aimX + 0.5) * nx + (aimY + 0.5) * ny;
        double tx = -ny, ty = nx;
        double along = (aimX + 0.5) * tx + (aimY + 0.5) * ty;
        double limit = small ? 1.6 : 1.0e9;
        boolean cutShape = false;
        int removed = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!stone[y][x]) {
                    continue;
                }
                if (small && Math.abs((x + 0.5) * tx + (y + 0.5) * ty - along) > limit) {
                    continue;
                }
                if ((x + 0.5) * nx + (y + 0.5) * ny > line) {
                    stone[y][x] = false;
                    removed++;
                    if (shape[y][x]) {
                        cutShape = true;
                    }
                }
            }
        }
        flash = 4;
        if (removed == 0) {
            blunder++;
        }
        if (cutShape) {
            blunder += 2;
        }
        if (blunder > MAX_BLUNDER) {
            PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, FlintItem.KNAP_FAIL));
            onClose();
            return;
        }
        check();
    }

    /** Лишнего кремня не осталось — считаем качество. */
    private void check() {
        int extra = 0, missing = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (stone[y][x] && !shape[y][x]) {
                    extra++;
                }
                if (!stone[y][x] && shape[y][x]) {
                    missing++;
                }
            }
        }
        if (extra <= 6) {
            finished = true;
            poor = extra > 0 || missing > 0;
        }
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
    }

    private void sendResult() {
        if (!finished || sent) {
            return;
        }
        sent = true;
        PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, poor ? FlintItem.KNAP_POOR : FlintItem.KNAP_GOOD));
        super.onClose();
    }

    /** Закрыли окно (Esc или крестик) с готовым результатом — головку всё равно отдаём. */
    @Override
    public void onClose() {
        if (finished && !sent) {
            sendResult();
            return;
        }
        super.onClose();
    }
}

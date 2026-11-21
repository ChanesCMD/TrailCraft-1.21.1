package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.FlintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Миниигра оббивки кремня. На плите лежит кусок кремня, поверх — контур головки из инвентаря.
 * Полоску откола двигают мышью и крутят на Q/E, пробел скалывает подсвеченные клетки,
 * Shift — малый скол (только поверхностный слой). Отколол лишнее — кремень испорчен;
 * оставил много лишнего — игра не заканчивается; снял почти всё, но не идеально — головка плохого качества.
 */
public class KnappingScreen extends Screen {
    private static final int GRID = 32;
    private static final int CELL = 6;
    private static final int BOARD = GRID * CELL;
    private static final int MAX_BLUNDER = 10;
    private static final double FINE = 1.6;

    /** Контуры головок — из текстур предметов (16x16), растягиваются на сетку. */
    private static final String[][] HEADS = {
            {"................", "................", "................", "................", "................",
             "................", "......###.......", ".....#####......", "......#####.....", "........##......",
             "................", "................", "................", "................", "................",
             "................"},
            {"................", "................", "................", "................", "................",
             "................", ".......###......", "......####......", "......####......", "......###.......",
             "................", "................", "................", "................", "................",
             "................"},
            {"................", "................", "................", "................", "................",
             "......###.......", ".....#####......", ".....######.....", "......####......", "........#.......",
             "................", "................", "................", "................", "................",
             "................"},
            {"................", "................", "................", "................", "......##........",
             ".....####.......", "....######......", "....#######.....", ".....#######....", ".........###....",
             ".........##.....", "................", "................", "................", "................",
             "................"},
            {"................", "................", "................", "................", "................",
             ".........###....", ".......####.....", "......####......", ".....####.......", "....####........",
             "....##..........", "................", "................", "................", "................",
             "................"},
            {"................", "................", "................", "..........##....", "........####....",
             "......#####.....", ".....######.....", ".....#####......", ".....#####......", "....#####.......",
             "...###..........", "...##...........", "................", "................", "................",
             "................"},
    };

    private final int tool;
    private final boolean[][] stone = new boolean[GRID][GRID];
    private final boolean[][] shape = new boolean[GRID][GRID];
    private final boolean[][] doomed = new boolean[GRID][GRID];
    private final List<Chip> chips = new ArrayList<>();
    private int blunder;
    private boolean finished;
    private boolean poor;
    private boolean sent;
    private int flash;
    private int shake;
    private double aimX = GRID / 2.0, aimY = -2;
    private float angle = 90f;
    private boolean small;

    private record Chip(double x, double y, double vx, double vy, int life, int size, int color) {}

    public KnappingScreen(int tool) {
        super(Component.translatable("gui.trailcraft.knapping"));
        this.tool = tool;
        buildShape();
        // заготовка — прямоугольная плита кремня, углы слегка сколоты
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                stone[y][x] = x >= 1 && y >= 1 && x < GRID - 1 && y < GRID - 1;
            }
        }
        // края неровные: выбиваем случайные клетки по периметру
        for (int i = 1; i < GRID - 1; i++) {
            if (hash(i * 37, 11) < 0.42) {
                stone[1][i] = false;
            }
            if (hash(i * 53, 29) < 0.42) {
                stone[GRID - 2][i] = false;
            }
            if (hash(7, i * 61) < 0.42) {
                stone[i][1] = false;
            }
            if (hash(41, i * 19) < 0.42) {
                stone[i][GRID - 2] = false;
            }
            if (hash(i * 23, 71) < 0.22) {
                stone[2][i] = false;
            }
            if (hash(83, i * 31) < 0.22) {
                stone[i][2] = false;
            }
        }
        // контур головки всегда лежит на кремне: иначе «недостающие» клетки считались бы с самого начала
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (shape[y][x]) {
                    stone[y][x] = true;
                }
            }
        }
    }

    public static void open(int tool) {
        Minecraft.getInstance().setScreen(new KnappingScreen(tool));
    }

    /** Контур головки берём из текстуры предмета: обрезаем по краям и растягиваем почти на всю сетку. */
    private void buildShape() {
        String[] art = HEADS[Math.min(tool, HEADS.length - 1)];
        int x0 = 16, y0 = 16, x1 = -1, y1 = -1;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (art[y].charAt(x) == '#') {
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    y0 = Math.min(y0, y);
                    y1 = Math.max(y1, y);
                }
            }
        }
        if (x1 < 0) {
            return;
        }
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        int span = GRID - 4;
        double scale = Math.min(span / (double) bw, span / (double) bh);
        int w = (int) Math.round(bw * scale), h = (int) Math.round(bh * scale);
        int ox = (GRID - w) / 2, oy = (GRID - h) / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = x0 + Math.min(bw - 1, (int) (x / scale));
                int sy = y0 + Math.min(bh - 1, (int) (y / scale));
                shape[oy + y][ox + x] = art[sy].charAt(sx) == '#';
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (width - BOARD) / 2;
    }

    private int top() {
        // панель выше и ниже плиты, центрируем всю панель целиком
        return (height - (BOARD + 92)) / 2 + 34;
    }

    private boolean exposed(int x, int y) {
        return x == 0 || y == 0 || x == GRID - 1 || y == GRID - 1
                || !stone[y][x - 1] || !stone[y][x + 1] || !stone[y - 1][x] || !stone[y + 1][x];
    }

    private static double noise(double x, double y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        double n00 = hash(xi, yi), n10 = hash(xi + 1, yi), n01 = hash(xi, yi + 1), n11 = hash(xi + 1, yi + 1);
        double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        double a = n00 + (n10 - n00) * sx, b = n01 + (n11 - n01) * sx;
        return a + (b - a) * sy;
    }

    private static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    private void collectDoomed() {
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                doomed[y][x] = false;
            }
        }
        if (finished) {
            return;
        }
        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);
        double tx = -ny, ty = nx;
        double line = (aimX + 0.5) * nx + (aimY + 0.5) * ny;
        double along = (aimX + 0.5) * tx + (aimY + 0.5) * ty;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!stone[y][x]) {
                    continue;
                }
                double px = x + 0.5, py = y + 0.5;
                if (px * nx + py * ny <= line) {
                    continue;
                }
                if (small && Math.abs(px * tx + py * ty - along) > FINE) {
                    continue;
                }
                if (small && !exposed(x, y)) {
                    continue;
                }
                doomed[y][x] = true;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        aimX = (mouseX - left()) / (double) CELL;
        aimY = (mouseY - top()) / (double) CELL;
        small = hasShiftDown();
        collectDoomed();

        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);
        double tx = -ny, ty = nx;
        int shakeX = shake > 0 ? (int) Math.round((Math.random() - 0.5) * 2) : 0;
        int shakeY = shake > 0 ? (int) Math.round((Math.random() - 0.5) * 2) : 0;
        int x0 = left() + shakeX, y0 = top() + shakeY;

        UiDraw.panel(g, x0 - 18, y0 - 34, BOARD + 36, BOARD + 92);
        g.fill(x0 - 18, y0 - 34, x0 + BOARD + 18, y0 - 18, 0x30F0F0F0);
        g.drawString(font, title, x0 - 10, y0 - 29, UiDraw.LABEL, false);
        g.renderItem(FlintItem.head(tool), x0 + BOARD - 10, y0 - 33);

        g.fill(x0, y0, x0 + BOARD, y0 + BOARD, 0xFF232323);
        UiDraw.frame(g, x0 - 1, y0 - 1, BOARD + 2, BOARD + 2, 0xFF191919, 0xFF565656, 0xFF191919);

        boolean pulse = (System.currentTimeMillis() / 200L) % 2 == 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int px = x0 + x * CELL, py = y0 + y * CELL;
                if (stone[y][x]) {
                    double n = noise(x * 0.55, y * 0.55);
                    double jitter = hash(x * 7 + 3, y * 11 + 5) * 0.12 - 0.06;
                    double shade = 0.72 + n * 0.45 + jitter;
                    boolean rim = x == 0 || y == 0 || x == GRID - 1 || y == GRID - 1
                            || !stone[y][x - 1] || !stone[y][x + 1] || !stone[y - 1][x] || !stone[y + 1][x];
                    if (rim) {
                        shade *= 0.82;
                    }
                    if (n > 0.78) {
                        shade *= 1.12;
                    }
                    if (n < 0.18) {
                        shade *= 0.7;
                    }
                    int r = (int) Math.min(255, 96 * shade);
                    int gg = (int) Math.min(255, 94 * shade);
                    int b = (int) Math.min(255, 88 * shade);
                    g.fill(px, py, px + CELL, py + CELL, 0xFF000000 | r << 16 | gg << 8 | b);
                    if (doomed[y][x]) {
                        g.fill(px, py, px + CELL, py + CELL, pulse ? 0x99C4442F : 0x80A03A28);
                    }
                } else if (shape[y][x]) {
                    g.fill(px, py, px + CELL, py + CELL, 0x26C8B080);
                }
            }
        }
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!shape[y][x]) {
                    continue;
                }
                int px = x0 + x * CELL, py = y0 + y * CELL;
                int c = finished ? 0xFF7BC67B : 0xFFD8A45A;
                if (y == 0 || !shape[y - 1][x]) {
                    g.fill(px, py, px + CELL, py + 1, c);
                }
                if (y == GRID - 1 || !shape[y + 1][x]) {
                    g.fill(px, py + CELL - 1, px + CELL, py + CELL, c);
                }
                if (x == 0 || !shape[y][x - 1]) {
                    g.fill(px, py, px + 1, py + CELL, c);
                }
                if (x == GRID - 1 || !shape[y][x + 1]) {
                    g.fill(px + CELL - 1, py, px + CELL, py + CELL, c);
                }
            }
        }
        double half = (small ? FINE : GRID / 2.0) * CELL;
        double mx = x0 + aimX * CELL, my = y0 + aimY * CELL;
        int lx = (int) Math.round(mx + tx * half), ly = (int) Math.round(my + ty * half);
        int rx = (int) Math.round(mx - tx * half), ry = (int) Math.round(my - ty * half);
        drawLine(g, lx + 1, ly + 1, rx + 1, ry + 1, 0x40000000);
        drawLine(g, lx, ly, rx, ry, flash > 0 ? 0xFFFFE08A : 0xFFF2F2F2);
        int ax = (int) Math.round(mx + nx * 9), ay = (int) Math.round(my + ny * 9);
        int bx = (int) Math.round(mx + nx * 18), by = (int) Math.round(my + ny * 18);
        drawLine(g, ax, ay, bx, by, 0xFFE8C070);
        drawLine(g, bx, by, bx - (int) Math.round(nx * 4 - tx * 3), by - (int) Math.round(ny * 4 - ty * 3), 0xFFE8C070);
        drawLine(g, bx, by, bx - (int) Math.round(nx * 4 + tx * 3), by - (int) Math.round(ny * 4 + ty * 3), 0xFFE8C070);

        for (Chip chip : chips) {
            int px = (int) Math.round(x0 + chip.x() * CELL), py = (int) Math.round(y0 + chip.y() * CELL);
            int alpha = Math.min(255, chip.life() * 26);
            g.fill(px, py, px + chip.size(), py + chip.size(), (alpha << 24) | (chip.color() & 0xFFFFFF));
        }

        g.drawString(font, Component.translatable("gui.trailcraft.knapping.angle", Math.round((angle % 360 + 360) % 360))
                        .append("  ").append(Component.translatable(small
                                ? "gui.trailcraft.knapping.small" : "gui.trailcraft.knapping.full")),
                x0 - 10, y0 + BOARD + 5, small ? 0xFFD8A45A : UiDraw.GREY, false);
        g.drawString(font, Component.translatable("gui.trailcraft.knapping.no_chips"),
                x0 - 10, y0 + BOARD + 16, 0xFF707070, false);
        if (finished) {
            Component text = Component.translatable(poor
                    ? "gui.trailcraft.knapping.poor" : "gui.trailcraft.knapping.good");
            g.drawString(font, text, x0 - 10, y0 + BOARD + 30, poor ? 0xFFD8A45A : 0xFF7BC67B, false);
        } else if (blunder > MAX_BLUNDER / 2) {
            g.drawString(font, Component.translatable("gui.trailcraft.knapping.cracking"),
                    x0 - 10, y0 + BOARD + 30, 0xFFC06050, false);
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
        if (keyCode == 81) {
            angle = (angle - step + 360f) % 360f;
            return true;
        }
        if (keyCode == 69) {
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

    private void chip() {
        small = hasShiftDown();
        collectDoomed();
        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);
        boolean cutShape = false;
        int removed = 0;
        int cutShapeCells = 0;
        int sx = 0, sy = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!doomed[y][x]) {
                    continue;
                }
                stone[y][x] = false;
                removed++;
                sx += x;
                sy += y;
                if (shape[y][x]) {
                    cutShape = true;
                    cutShapeCells++;
                }
            }
        }
        if (removed == 0) {
            flash = 2;
            return;
        }
        spawnChips(sx / (double) removed, sy / (double) removed, nx, ny, Math.min(10, 2 + removed));
        flash = 4;
        shake = cutShape ? 5 : 2;
        playSound(cutShape ? 0.6f : 1.0f);
        if (cutShape) {
            blunder += 2;
        }
        // срубил контур — кремень испорчен сразу: половину головки снести и «доиграть» нельзя
        if (cutShapeCells > 4 || countMissing() > 12 || blunder > MAX_BLUNDER) {
            PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, FlintItem.KNAP_FAIL));
            sent = true;
            super.onClose();
            return;
        }
        check();
    }

    /** Сколько клеток контура уже снесено. */
    private int countMissing() {
        int missing = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (shape[y][x] && !stone[y][x]) {
                    missing++;
                }
            }
        }
        return missing;
    }

    private void spawnChips(double x, double y, double nx, double ny, int count) {
        for (int i = 0; i < count && chips.size() < 120; i++) {
            chips.add(new Chip(x + (Math.random() - 0.5), y + (Math.random() - 0.5),
                    nx * 0.16 + (Math.random() - 0.5) * 0.06, ny * 0.16 + (Math.random() - 0.5) * 0.06 + 0.02,
                    8 + (int) (Math.random() * 5), 1 + (int) (Math.random() * 2), 0xB8B4AC));
        }
    }

    private void playSound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_HIT, pitch, 0.7f));
    }

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
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.STONE_BUTTON_CLICK_ON, poor ? 1.0f : 1.4f, 0.6f));
        }
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
        if (shake > 0) {
            shake--;
        }
        List<Chip> alive = new ArrayList<>();
        for (Chip chip : chips) {
            Chip moved = new Chip(chip.x() + chip.vx(), chip.y() + chip.vy(), chip.vx(), chip.vy() + 0.02,
                    chip.life() - 1, chip.size(), chip.color());
            if (moved.life() > 0) {
                alive.add(moved);
            }
        }
        chips.clear();
        chips.addAll(alive);
    }

    private void sendResult() {
        if (!finished || sent) {
            return;
        }
        sent = true;
        PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, poor ? FlintItem.KNAP_POOR : FlintItem.KNAP_GOOD));
        super.onClose();
    }

    @Override
    public void onClose() {
        if (finished && !sent) {
            sendResult();
            return;
        }
        super.onClose();
    }
}

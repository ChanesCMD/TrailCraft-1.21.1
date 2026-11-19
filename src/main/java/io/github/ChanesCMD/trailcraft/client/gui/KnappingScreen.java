package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.FlintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Миниигра оббивки кремня: на плите лежит кусок кремня, поверх — контур головки.
 * Полоску (линию откола) крутят на Q и E, пробел откалывает крайний слой кремня вдоль полоски.
 * Отколол лишнее — кремень испорчен; оставил много лишнего — игра не заканчивается;
 * срезал почти всё, но не идеально — головка плохого качества.
 */
public class KnappingScreen extends Screen {
    private static final int GRID = 16;
    private static final int CELL = 7;
    private static final int BOARD = GRID * CELL;
    /** Сколько клеток контура можно срезать, прежде чем кремень считается испорченным. */
    private static final int MAX_BLUNDER = 8;

    private static final String[][] SHAPES = {
            // PICKAXE
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
            // AXE
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
            // SHOVEL
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
            // HOE
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
            // KNIFE
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
            // SPEAR
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
    private final int head;
    private final boolean[][] stone = new boolean[GRID][GRID];
    private final boolean[][] shape = new boolean[GRID][GRID];
    private float angle = 35f;
    private int blunder;
    private boolean finished;
    private boolean poor;
    private float flash;
    private int chips;

    public KnappingScreen(int tool) {
        super(Component.translatable("gui.trailcraft.knapping"));
        this.tool = tool;
        this.head = 0;
        String[] art = SHAPES[Math.min(tool, SHAPES.length - 1)];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                shape[y][x] = art[y].charAt(x) == '#';
            }
        }
        // кусок кремня — скруглённый брусок на всю плиту
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                float dx = (x - (GRID - 1) / 2f) / (GRID / 2f - 0.5f);
                float dy = (y - (GRID - 1) / 2f) / (GRID / 2f - 0.5f);
                stone[y][x] = dx * dx + dy * dy <= 0.92f;
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
        return (width - BOARD) / 2 - 46;
    }

    private int top() {
        return (height - BOARD) / 2 - 6;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = left(), y0 = top();
        UiDraw.panel(g, x0 - 14, y0 - 26, BOARD + 28, BOARD + 76);
        g.drawString(font, title, x0 - 4, y0 - 19, UiDraw.LABEL, false);
        g.drawString(font, Component.translatable("gui.trailcraft.knapping.head").append(FlintItem.head(tool).getHoverName()),
                x0 - 4, y0 - 8, UiDraw.GREY, false);

        // плита-подложка
        g.fill(x0, y0, x0 + BOARD, y0 + BOARD, 0xFF2B2B2B);
        UiDraw.frame(g, x0 - 1, y0 - 1, BOARD + 2, BOARD + 2, 0xFF1A1A1A, 0xFF4A4A4A, 0xFF1A1A1A);

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int px = x0 + x * CELL;
                int py = y0 + y * CELL;
                if (stone[y][x]) {
                    // кремень: серо-коричневый, с «щербинками»
                    int shade = ((x * 31 + y * 17) % 5) == 0 ? 0xFF8C8C93 : 0xFF9A9AA0;
                    g.fill(px, py, px + CELL, py + CELL, shade);
                } else if (shape[y][x]) {
                    // след контура
                    g.fill(px, py, px + CELL, py + CELL, 0x30C8B080);
                }
            }
        }
        // контур головки
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!shape[y][x]) {
                    continue;
                }
                int px = x0 + x * CELL;
                int py = y0 + y * CELL;
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
        // полоска откола
        double rad = Math.toRadians(angle);
        double cx = x0 + BOARD / 2.0, cy = y0 + BOARD / 2.0;
        double nx = Math.cos(rad), ny = Math.sin(rad);
        double px = -ny, py = nx;
        int len = BOARD;
        int ax = (int) Math.round(cx - px * len / 2), ay = (int) Math.round(cy - py * len / 2);
        int bx = (int) Math.round(cx + px * len / 2), by = (int) Math.round(cy + py * len / 2);
        drawLine(g, ax, ay, bx, by, flash > 0 ? 0xFFFFE28A : 0xFFE8E8E8);

        g.drawString(font, Component.translatable("gui.trailcraft.knapping.angle", Math.round(angle % 360)),
                x0 - 4, y0 + BOARD + 6, UiDraw.GREY, false);
        g.drawString(font, Component.translatable("gui.trailcraft.knapping.keys"),
                x0 - 4, y0 + BOARD + 18, 0xFF707070, false);
        if (finished) {
            Component text = Component.translatable(poor
                    ? "gui.trailcraft.knapping.poor" : "gui.trailcraft.knapping.good");
            g.drawString(font, text, x0 - 4, y0 + BOARD + 32, poor ? 0xFFD8A45A : 0xFF7BC67B, false);
        } else if (blunder > MAX_BLUNDER / 2) {
            g.drawString(font, Component.translatable("gui.trailcraft.knapping.cracking"),
                    x0 - 4, y0 + BOARD + 32, 0xFFC06050, false);
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1, y = y1;
        for (int i = 0; i < 600; i++) {
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
        // Q и E — вращать полоску
        if (keyCode == 81) {
            angle = (angle - 7.5f + 360f) % 360f;
            return true;
        }
        if (keyCode == 69) {
            angle = (angle + 7.5f) % 360f;
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

    /** Отколоть крайний слой кремня со стороны, куда смотрит полоска. */
    private void chip() {
        double rad = Math.toRadians(angle);
        double nx = Math.cos(rad), ny = Math.sin(rad);
        double max = Double.NEGATIVE_INFINITY;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (stone[y][x]) {
                    max = Math.max(max, x * nx + y * ny);
                }
            }
        }
        if (max == Double.NEGATIVE_INFINITY) {
            check();
            return;
        }
        boolean cutShape = false;
        int removed = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (stone[y][x] && x * nx + y * ny > max - 1.35) {
                    stone[y][x] = false;
                    removed++;
                    if (shape[y][x]) {
                        cutShape = true;
                    }
                }
            }
        }
        chips++;
        flash = 6f;
        if (removed == 0) {
            blunder++;
        }
        if (cutShape) {
            blunder += 2;
        }
        if (blunder > MAX_BLUNDER) {
            finished = true;
            poor = false;
            // слишком много отколото — провал
            PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, FlintItem.KNAP_FAIL));
            onClose();
            return;
        }
        check();
    }

    /** Пора заканчивать? Лишнего кремня не осталось — считаем качество. */
    private void check() {
        int extra = 0;
        int missing = 0;
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
        // чуть-чуть лишнего кремня осталось — головка выйдет плохого качества
        if (extra <= 2) {
            finished = true;
            poor = extra > 0 || missing > 0;
        }
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash -= 1f;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (finished) {
            sendResult();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendResult() {
        if (!finished) {
            return;
        }
        PacketDistributor.sendToServer(new FlintItem.KnapPayload(tool, poor ? FlintItem.KNAP_POOR : FlintItem.KNAP_GOOD));
        onClose();
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}

package io.github.ChanesCMD.trailcraft.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.smelting.ClayMoldItem;
import io.github.ChanesCMD.trailcraft.smelting.ClayShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;

/**
 * Миниигра лепки из глины: на плоскость-заготовку надо налепить глину по форме.
 * ЛКМ — добавить глину, ПКМ — убрать; запас глины ограничен. Заполнил форму
 * с небольшим излишком — слепок готов; глина кончилась — закрой и попробуй снова.
 */
public class ClayMoldingScreen extends Screen {
    private static final int GRID = 16;
    private static final int CELL = 10;
    private static final int BOARD = GRID * CELL;
    private static final int PAD = 16;
    private static final int WIDTH = BOARD + PAD * 2;
    private static final int HEIGHT = BOARD + 100;
    private static final int MAX_EXTRA = 6;

    private final int shapeIndex;
    private final ClayShape shape;
    private final boolean[][] target = new boolean[GRID][GRID];
    private final int[][] required = new int[GRID][GRID];
    private final int[][] layers = new int[GRID][GRID];
    private final boolean[][] stroke = new boolean[GRID][GRID];
    private ResourceLocation texture;
    private int requiredTotal;
    private int supply;
    private boolean finished;
    private boolean failed;
    private boolean sent;

    public ClayMoldingScreen(int shapeIndex) {
        super(Component.translatable("gui.trailcraft.clay_molding"));
        this.shapeIndex = shapeIndex;
        this.shape = ClayShape.values()[shapeIndex];
        loadTarget();
        this.supply = this.requiredTotal + 24;
    }

    public static void open(int shapeIndex) {
        Minecraft.getInstance().setScreen(new ClayMoldingScreen(shapeIndex));
    }

    /** Форма — из текстуры сырой формы: тёмное углубление инструмента (у кувшина — весь силуэт). */
    private void loadTarget() {
        texture = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "textures/item/unfired_" + shape.id + ".png");
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return;
        }
        boolean[][] dark = new boolean[GRID][GRID];
        boolean[][] opaque = new boolean[GRID][GRID];
        int darkCount = 0;
        try (var in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            for (int y = 0; y < GRID; y++) {
                for (int x = 0; x < GRID; x++) {
                    int argb = image.getPixelRGBA(x, y);
                    int a = argb >>> 24;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    boolean inside = x > 0 && y > 0 && x < GRID - 1 && y < GRID - 1;
                    opaque[y][x] = a > 0;
                    if (a > 0 && inside && (r + g + b) / 3 < 120) {
                        dark[y][x] = true;
                        darkCount++;
                    }
                }
            }
        } catch (IOException ignored) {
            return;
        }
        boolean useDark = shape != ClayShape.JUG && darkCount >= 8;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                target[y][x] = useDark ? dark[y][x] : opaque[y][x];
            }
        }
        // Края формы — два слоя, середина — один.
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (!target[y][x]) {
                    continue;
                }
                boolean edge = false;
                for (int dy = -1; dy <= 1 && !edge; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID || !target[ny][nx]) {
                            edge = true;
                            break;
                        }
                    }
                }
                required[y][x] = edge ? 2 : 1;
                requiredTotal += required[y][x];
            }
        }
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    private int boardX() {
        return left() + PAD;
    }

    private int boardY() {
        return top() + 30;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiDraw.panel(g, left(), top(), WIDTH, HEIGHT);
        g.drawString(font, title, left() + 8, top() + 7, UiDraw.LABEL, false);
        g.renderItem(new ItemStack(shape.result()), left() + WIDTH - 22, top() + 4);

        int x0 = boardX(), y0 = boardY();
        g.fill(x0 - 2, y0 - 2, x0 + BOARD + 2, y0 + BOARD + 2, 0xFF232323);
        UiDraw.frame(g, x0 - 3, y0 - 3, BOARD + 6, BOARD + 6, 0xFF191919, 0xFF565656, 0xFF191919);
        // чёрный фон плоскости
        g.fill(x0, y0, x0 + BOARD, y0 + BOARD, 0xFF000000);

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int px = x0 + x * CELL, py = y0 + y * CELL;
                int req = required[y][x];
                int have = layers[y][x];
                if (req > 0) {
                    if (have > 0) {
                        int color = req == 2 && have < 2 ? 0xFF7A4E32 : 0xFFA0785A;
                        g.fill(px, py, px + CELL, py + CELL, color);
                        if (have >= 2) {
                            g.fill(px + 2, py + 2, px + CELL - 2, py + CELL - 2, 0xFFC08A5E);
                        }
                        if (have > req) {
                            g.fill(px, py, px + CELL, py + CELL, 0x55C06050);
                        }
                    }
                    int col = req == 2 ? 0xFFE0E0E0 : 0xFF808080;
                    border(g, px, py, CELL, col);
                    if (req == 2) {
                        border(g, px + 1, py + 1, CELL - 2, col);
                        if (have == 1) {
                            g.fill(px + CELL / 2 - 1, py + CELL / 2 - 1,
                                    px + CELL / 2 + 1, py + CELL / 2 + 1, 0xFFFFE08A);
                        }
                    }
                } else if (have > 0) {
                    g.fill(px, py, px + CELL, py + CELL, 0xFFC06050);
                    g.fill(px + 2, py + 2, px + CELL - 2, py + CELL - 2, 0xFFD08070);
                }
            }
        }

        int mx = (int) Math.floor((mouseX - x0) / (double) CELL);
        int my = (int) Math.floor((mouseY - y0) / (double) CELL);
        if (!finished && !failed && mx >= 0 && my >= 0 && mx < GRID && my < GRID) {
            int px = x0 + mx * CELL, py = y0 + my * CELL;
            UiDraw.frame(g, px, py, CELL, CELL, 0xFF000000, 0xFFFFFFFF, 0xFFFFE08A);
        }

        int missing = 0, extra = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int req = required[y][x];
                int have = layers[y][x];
                if (have > req) {
                    extra += have - req;
                } else {
                    missing += req - have;
                }
            }
        }
        int ty = y0 + BOARD + 6;
        g.drawString(font, Component.translatable("gui.trailcraft.clay_molding.supply", supply), x0, ty, UiDraw.GREY, false);
        g.drawString(font, Component.translatable("gui.trailcraft.clay_molding.extra", extra), x0, ty + 11,
                extra > MAX_EXTRA ? 0xFFC06050 : UiDraw.GREY, false);
        g.drawString(font, Component.translatable("gui.trailcraft.clay_molding.missing", missing), x0, ty + 22,
                missing > 0 ? 0xFFD8A45A : 0xFF7BC67B, false);

        Component hint;
        int color;
        if (finished) {
            hint = Component.translatable("gui.trailcraft.clay_molding.done");
            color = 0xFF7BC67B;
        } else if (failed) {
            hint = Component.translatable("gui.trailcraft.clay_molding.failed");
            color = 0xFFC06050;
        } else {
            hint = Component.translatable("gui.trailcraft.clay_molding.hint");
            color = 0xFF707070;
        }
        g.drawString(font, hint, x0, ty + 36, color, false);
        g.drawString(font, Component.translatable("gui.trailcraft.clay_molding.controls"), x0, ty + 47,
                0xFF707070, false);
    }

    private static void border(GuiGraphics g, int x, int y, int size, int color) {
        g.fill(x, y, x + size, y + 1, color);
        g.fill(x, y + size - 1, x + size, y + size, color);
        g.fill(x, y + 1, x + 1, y + size - 1, color);
        g.fill(x + size - 1, y + 1, x + size, y + size - 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (finished) {
            sendResult();
            return true;
        }
        if (failed) {
            onClose();
            return true;
        }
        clearStroke();
        paint(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (finished || failed) {
            return true;
        }
        paint(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        clearStroke();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clearStroke() {
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                stroke[y][x] = false;
            }
        }
    }

    private void paint(double mouseX, double mouseY, int button) {
        int x = (int) Math.floor((mouseX - boardX()) / (double) CELL);
        int y = (int) Math.floor((mouseY - boardY()) / (double) CELL);
        if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
            return;
        }
        if (button == 0) {
            // за одно нажатие/протяжку клетка получает только один слой
            if (!stroke[y][x] && layers[y][x] < 2 && supply > 0) {
                layers[y][x]++;
                stroke[y][x] = true;
                supply--;
                play(SoundEvents.MUD_PLACE, 1.3f);
                check();
            }
        } else if (button == 1) {
            if (layers[y][x] > 0) {
                layers[y][x]--;
                play(SoundEvents.MUD_BREAK, 1.5f);
                check();
            }
        }
    }

    private void check() {
        int missing = 0, extra = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int req = required[y][x];
                int have = layers[y][x];
                if (have > req) {
                    extra += have - req;
                } else {
                    missing += req - have;
                }
            }
        }
        if (missing == 0 && extra <= MAX_EXTRA) {
            finished = true;
            play(SoundEvents.MUD_PLACE, 0.8f);
        } else if (supply <= 0 && missing > 0) {
            failed = true;
            play(SoundEvents.MUD_BREAK, 0.8f);
        }
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.7f));
    }

    private void sendResult() {
        if (!finished || sent) {
            return;
        }
        sent = true;
        PacketDistributor.sendToServer(new ClayMoldItem.ShapePayload(shapeIndex));
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32 && finished) {
            sendResult();
            return true;
        }
        if (keyCode == 256 && finished) {
            sendResult();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

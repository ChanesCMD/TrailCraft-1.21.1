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
    private static final int HEIGHT = BOARD + 90;
    private static final int MAX_EXTRA = 6;

    private final int shapeIndex;
    private final ClayShape shape;
    private final boolean[][] target = new boolean[GRID][GRID];
    private final boolean[][] clay = new boolean[GRID][GRID];
    private ResourceLocation texture;
    private int targetCount;
    private int supply;
    private boolean finished;
    private boolean failed;
    private boolean sent;

    public ClayMoldingScreen(int shapeIndex) {
        super(Component.translatable("gui.trailcraft.clay_molding"));
        this.shapeIndex = shapeIndex;
        this.shape = ClayShape.values()[shapeIndex];
        loadTarget();
        this.supply = this.targetCount + 24;
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
                if (target[y][x]) {
                    targetCount++;
                }
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
        if (texture != null) {
            g.blit(texture, x0, y0, 0, 0, BOARD, BOARD, GRID, GRID);
        }

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int px = x0 + x * CELL, py = y0 + y * CELL;
                if (target[y][x] && !clay[y][x]) {
                    g.fill(px, py, px + CELL, py + CELL, 0x35D8C090);
                }
                if (clay[y][x]) {
                    g.fill(px, py, px + CELL, py + CELL, target[y][x] ? 0xFFA0785A : 0xFFC06050);
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
                if (target[y][x] && !clay[y][x]) {
                    missing++;
                }
                if (clay[y][x] && !target[y][x]) {
                    extra++;
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

    private void paint(double mouseX, double mouseY, int button) {
        int x = (int) Math.floor((mouseX - boardX()) / (double) CELL);
        int y = (int) Math.floor((mouseY - boardY()) / (double) CELL);
        if (x < 0 || y < 0 || x >= GRID || y >= GRID) {
            return;
        }
        if (button == 0) {
            if (!clay[y][x] && supply > 0) {
                clay[y][x] = true;
                supply--;
                play(SoundEvents.MUD_PLACE, 1.3f);
                check();
            }
        } else if (button == 1 && clay[y][x]) {
            clay[y][x] = false;
            play(SoundEvents.MUD_BREAK, 1.5f);
            check();
        }
    }

    private void check() {
        int missing = 0, extra = 0;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (target[y][x] && !clay[y][x]) {
                    missing++;
                }
                if (clay[y][x] && !target[y][x]) {
                    extra++;
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

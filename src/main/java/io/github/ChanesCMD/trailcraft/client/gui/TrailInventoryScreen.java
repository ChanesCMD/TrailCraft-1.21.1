package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.*;

/**
 * Экран инвентаря (ТЗ, часть 1, разделы 1–2). Макеты: docs/ui/01_inventory_default.png, 02_inventory_warmth.png.
 * Слева от панели — панель голосового чата (головы появятся на шаге 7).
 */
public class TrailInventoryScreen extends AbstractContainerScreen<TrailInventoryMenu> {
    private static final int VOICE_W = 32, VOICE_H = 125, VOICE_GAP = 6;
    /** Насколько панель поднята над центром, чтобы под кнопкой календаря влезло его окно (на 640×360 верх на y=78). */
    private static final int RAISE = 16;
    private static final int CALENDAR_X = 279, CALENDAR_Y = 95, CALENDAR_W = 102, CALENDAR_H = 20;
    /** Фон столбика «Тепло» (x=60..84, y=32..110). */
    private static final int WARMTH_BG_X = 60, WARMTH_BG_Y = 32, WARMTH_BG_W = 24, WARMTH_BG_H = 78;

    private final Inventory playerInventory;

    public TrailInventoryScreen(TrailInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventory = playerInventory;
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.titleLabelX = MAIN_X;
        this.titleLabelY = MAIN_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int groupWidth = VOICE_W + VOICE_GAP + WIDTH;
        leftPos = Math.max(4, (width - groupWidth) / 2) + VOICE_W + VOICE_GAP;
        topPos = Math.max(4, (height - HEIGHT) / 2 - RAISE);

        // TODO (шаг 4): активна, только если в рюкзаке есть календарь; открывает окно календаря
        Button calendar = Button.builder(Component.translatable("gui.trailcraft.calendar"), b -> {})
                .bounds(leftPos + CALENDAR_X, topPos + CALENDAR_Y, CALENDAR_W, CALENDAR_H)
                .tooltip(Tooltip.create(Component.translatable("gui.trailcraft.calendar.need")))
                .build();
        calendar.active = false;
        addRenderableWidget(calendar);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        menu.warmthVisible = isOver(mouseX, mouseY, ARMOR_X, EQUIP_Y, 18, 72)
                || menu.warmthVisible && isOver(mouseX, mouseY, WARMTH_BG_X, WARMTH_BG_Y, WARMTH_BG_W, WARMTH_BG_H);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        UiDraw.panel(g, x - VOICE_GAP - VOICE_W, y, VOICE_W, VOICE_H);
        // TODO (шаг 7): значок микрофона и головы говоривших игроков
        for (int i = 0; i < 5; i++) {
            UiDraw.slot(g, x - VOICE_GAP - VOICE_W + 7, y + 19 + i * 20);
        }

        UiDraw.panel(g, x, y, WIDTH, HEIGHT);
        for (int i = 0; i < 4; i++) {
            UiDraw.slot(g, x + COSMETIC_X, y + EQUIP_Y + i * 18);
            UiDraw.slot(g, x + ARMOR_X, y + EQUIP_Y + i * 18);
        }
        UiDraw.frame(g, x + MODEL_X, y + MODEL_Y, MODEL_W, MODEL_H, 0xFF373737, 0xFFFFFFFF, 0xFF000000);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + MODEL_X + 1, y + MODEL_Y + 1,
                x + MODEL_X + MODEL_W - 1, y + MODEL_Y + MODEL_H - 1, 30, 0.0625f, mouseX, mouseY, playerInventory.player);

        if (menu.warmthVisible) {
            // Поверх модельки (она рисуется на z≈50), но под предметами (z≈150).
            // TODO: анимация выезда (~100–150 мс) по желанию; сейчас просто появляется
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            UiDraw.panel(g, x + WARMTH_BG_X, y + WARMTH_BG_Y, WARMTH_BG_W, WARMTH_BG_H);
            for (int i = 0; i < 4; i++) {
                UiDraw.slot(g, x + WARMTH_X, y + EQUIP_Y + i * 18);
            }
            g.pose().popPose();
        }

        for (int i = 0; i < 8; i++) {
            UiDraw.slot(g, x + ACCESSORY_X + (i % 4) * 18, y + ACCESSORY_Y + (i / 4) * 18);
        }
        for (int slotX : new int[]{WALLET_X, BELT_X, BACKPACK_X, OFFHAND_X}) {
            UiDraw.slot(g, x + slotX, y + BOTTOM_Y);
        }
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                UiDraw.slot(g, x + MAIN_X + col * 18, y + MAIN_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            UiDraw.slot(g, x + MAIN_X + col * 18, y + HOTBAR_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                UiDraw.slot(g, x + CRAFT_X + col * 18, y + CRAFT_Y + row * 18);
            }
        }
        UiDraw.arrow(g, x + 337, y + 55);
        UiDraw.slot(g, x + RESULT_X, y + RESULT_Y);
    }

    /** Переключатели косметики и кнопки окон рисуются поверх предметов в слотах. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, UiDraw.LABEL, false);
        g.drawString(font, Component.translatable("container.crafting"), CRAFT_X, titleLabelY, UiDraw.LABEL, false);
        if (menu.warmthVisible) {
            g.pose().pushPose();
            g.pose().translate(WARMTH_BG_X + WARMTH_BG_W / 2f, WARMTH_BG_Y - 7, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            Component warmth = Component.translatable("gui.trailcraft.warmth");
            g.drawString(font, warmth, -font.width(warmth) / 2, 0, UiDraw.LABEL, false);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (int i = 0; i < 4; i++) {
            UiDraw.circle(g, COSMETIC_X + 12, EQUIP_Y + i * 18 + 1, 5, menu.isCosmeticShown(i) ? UiDraw.GREEN : 0xFF6E6E6E);
        }
        // TODO (шаг 3): кнопки открывают окна кошелька, пояса и рюкзака
        for (int slotX : new int[]{WALLET_X, BELT_X, BACKPACK_X}) {
            UiDraw.plusButton(g, slotX + 10, BOTTOM_Y + 1);
        }
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 4; i++) {
            if (button == 0 && isOver(mouseX, mouseY, COSMETIC_X + 12, EQUIP_Y + i * 18 + 1, 5, 5)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Попадание в прямоугольник, заданный относительно панели. */
    private boolean isOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        return rx >= x && rx < x + w && ry >= y && ry < y + h;
    }

    /** Клик по панели голосового чата не считается кликом мимо окна (иначе предмет с курсора выбросится). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        boolean onVoice = mouseX >= left - VOICE_GAP - VOICE_W && mouseX < left && mouseY >= top && mouseY < top + VOICE_H;
        return !onVoice && super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }
}

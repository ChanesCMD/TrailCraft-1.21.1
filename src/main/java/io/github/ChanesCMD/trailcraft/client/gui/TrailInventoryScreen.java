package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceClient;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceHeads;
import io.github.ChanesCMD.trailcraft.inventory.DeathLogPayloads;
import io.github.ChanesCMD.trailcraft.inventory.DeathRecord;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.MenuSlot;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.Popup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.*;

/**
 * Экран инвентаря (ТЗ, часть 1, разделы 1–3). Макеты: docs/ui/01_inventory_default.png, 02_inventory_warmth.png, 04_backpack.png.
 * Слева от панели — головы голосового чата (только когда в нём хотя бы двое), в левом верхнем углу экрана — 5 кнопок.
 * Окна (рюкзак, журнал смертей, голосовой чат) рисуются поверх инвентаря с лёгким затемнением; закрываются Esc,
 * повторным нажатием кнопки или кликом вне окна.
 */
public class TrailInventoryScreen extends AbstractContainerScreen<TrailInventoryMenu> {
    /** Головы голосового чата слева от панели: отступ, ширина столбика с обводкой, верх первой головы. */
    private static final int HEADS_GAP = 6, HEADS_W = VoiceHeads.SIZE + 2, HEADS_Y = 20;
    /** Насколько панель поднята над центром, чтобы под кнопкой календаря влезло его окно (на 640×360 верх на y=78). */
    private static final int RAISE = 16;
    private static final int CALENDAR_X = 279, CALENDAR_Y = 95, BUTTON_W = 102, BUTTON_H = 20;
    private static final int DEATHS_BUTTON_Y = 119;
    /** Фон столбика «Тепло» (x=60..84, y=32..110). */
    private static final int WARMTH_BG_X = 60, WARMTH_BG_Y = 32, WARMTH_BG_W = 24, WARMTH_BG_H = 78;
    /** Окно журнала смертей поверх основного инвентаря. */
    private static final int DEATHS_X = 74, DEATHS_Y = 11, DEATHS_W = 240, DEATHS_H = 150, DEATH_ROW_H = 31;
    private static final int[] BAR_COLORS = {0xFFE8C33A, 0, 0xFF4A7BE0, 0, 0, 0, 0};
    private static final String[] BACKPACK_ROWS = {"battery", "solar", "mana", "sleeping", "workbench", "atlas", "calendar"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    /** Слои по глубине: предметы в слотах ≈150–200, затемнение и окна выше, предмет на курсоре ≈380. */
    /** Кнопки окон (+) лежат выше затемнения и окон: ими же окно и закрывается. */
    private static final int Z_BUTTONS = 210, Z_DIM = 220, Z_POPUP = 230, Z_POPUP_ITEMS = 100, Z_TOP_BUTTONS = 300;

    private static final int Z_OVERLAY = 400;

    private final Inventory playerInventory;
    private Button calendarButton;
    private List<CornerButtons.Entry> cornerButtons = List.of();
    private VoiceChatWindow voiceWindow;
    private boolean voiceOpen;
    private List<DeathRecord> deaths = List.of();
    private int deathsTotal;
    private long deathsNow;
    private int deathsScroll;

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
        leftPos = Math.max(4 + HEADS_W + HEADS_GAP, (width - WIDTH) / 2);
        topPos = Math.max(4, (height - HEIGHT) / 2 - RAISE);
        cornerButtons = CornerButtons.create(this, this::toggleVoice);
        voiceWindow = new VoiceChatWindow(font);
        for (int i = 0; i < cornerButtons.size(); i++) {
            if (cornerButtons.get(i).id().equals("voice")) {
                voiceWindow.place(CornerButtons.x(i), width, height);
            }
        }

        // TODO (шаг 4): открывает окно календаря
        calendarButton = addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.calendar"), b -> {})
                .bounds(leftPos + CALENDAR_X, topPos + CALENDAR_Y, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.deaths"), b -> toggleDeaths())
                .bounds(leftPos + CALENDAR_X, topPos + DEATHS_BUTTON_Y, BUTTON_W, BUTTON_H).build());
        updateCalendarButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateCalendarButton();
    }

    private void updateCalendarButton() {
        boolean has = menu.hasCalendar();
        calendarButton.active = has;
        calendarButton.setTooltip(has ? null : Tooltip.create(Component.translatable("gui.trailcraft.calendar.need")));
    }

    // === Окна ===

    private void openPopup(Popup popup) {
        menu.popup = popup;
        switch (popup) {
            case BACKPACK -> setPopupRect(BACKPACK_WIN_X, BACKPACK_WIN_Y, BACKPACK_W, BACKPACK_H);
            case DEATHS -> setPopupRect(DEATHS_X, DEATHS_Y, DEATHS_W, DEATHS_H);
            default -> setPopupRect(0, 0, 0, 0);
        }
    }

    private void setPopupRect(int x, int y, int w, int h) {
        menu.popupX = x;
        menu.popupY = y;
        menu.popupW = w;
        menu.popupH = h;
    }

    private void toggleDeaths() {
        if (menu.popup == Popup.DEATHS) {
            openPopup(Popup.NONE);
        } else {
            deaths = List.of();
            deathsScroll = 0;
            openPopup(Popup.DEATHS);
            PacketDistributor.sendToServer(DeathLogPayloads.Request.INSTANCE);
        }
    }

    public void showDeathLog(DeathLogPayloads.Log log) {
        List<DeathRecord> newestFirst = new ArrayList<>(log.records());
        Collections.reverse(newestFirst);
        deaths = newestFirst;
        deathsTotal = log.total();
        deathsNow = log.now();
    }

    private void toggleVoice() {
        voiceOpen = !voiceOpen;
        if (voiceOpen) {
            openPopup(Popup.NONE);
        }
    }

    private boolean overPopup(double mouseX, double mouseY) {
        return menu.popup != Popup.NONE && isOver(mouseX, mouseY, menu.popupX, menu.popupY, menu.popupW, menu.popupH);
    }

    // === Отрисовка ===

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        menu.warmthVisible = menu.popup == Popup.NONE && (isOver(mouseX, mouseY, ARMOR_X, EQUIP_Y, 18, 72)
                || menu.warmthVisible && isOver(mouseX, mouseY, WARMTH_BG_X, WARMTH_BG_Y, WARMTH_BG_W, WARMTH_BG_H));
        super.render(g, mouseX, mouseY, partialTick);
        // Кнопки в углу и окно голосового чата — поверх всего (в координатах экрана)
        g.pose().pushPose();
        g.pose().translate(0, 0, Z_OVERLAY);
        if (voiceOpen) {
            g.fill(0, 0, width, height, 0x40000000);
            voiceWindow.render(g, mouseX, mouseY);
        }
        CornerButtons.render(g, font, cornerButtons, mouseX, mouseY, voiceOpen ? voiceButton() : -1);
        g.pose().popPose();
        if (!voiceOpen) {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    private int voiceButton() {
        for (int i = 0; i < cornerButtons.size(); i++) {
            if (cornerButtons.get(i).id().equals("voice")) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        if (VoiceClient.showHeads()) {
            VoiceHeads.column(g, x - HEADS_GAP - HEADS_W + 1, y + HEADS_Y, VoiceClient.heads());
        }

        UiDraw.panel(g, x, y, WIDTH, HEIGHT);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i == RESULT || slot instanceof MenuSlot s && s.group() != GROUP_BASE) {
                continue;
            }
            if (slot instanceof MenuSlot s && s.isLocked()) {
                UiDraw.lockedSlot(g, x + slot.x - 1, y + slot.y - 1);
            } else {
                UiDraw.slot(g, x + slot.x - 1, y + slot.y - 1);
            }
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
        UiDraw.arrow(g, x + 337, y + 55);
        UiDraw.slot(g, x + RESULT_X, y + RESULT_Y);
    }

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

        // Переключатели косметики поверх предметов в слотах: включённый — светло-серый с обводкой
        g.pose().pushPose();
        g.pose().translate(0, 0, Z_BUTTONS);
        for (int i = 0; i < 4; i++) {
            if (menu.isCosmeticShown(i)) {
                UiDraw.outlinedCircle(g, COSMETIC_X + 12, EQUIP_Y + i * 18 + 1, 5, 0xFFE0E0E0, 0xFF373737);
            } else {
                UiDraw.circle(g, COSMETIC_X + 12, EQUIP_Y + i * 18 + 1, 5, 0xFF6E6E6E);
            }
        }
        g.pose().popPose();

        if (menu.popup != Popup.NONE) {
            g.pose().pushPose();
            g.pose().translate(0, 0, Z_DIM);
            g.fill(-leftPos, -topPos, width - leftPos, height - topPos, 0x40000000);
            g.pose().translate(0, 0, Z_POPUP - Z_DIM);
            if (menu.popup == Popup.BACKPACK) {
                renderBackpack(g);
            } else {
                renderDeaths(g);
            }
            g.pose().popPose();
        }

        // Кнопки окон на кошельке, поясе и рюкзаке — на самом верхнем слое, над предметами, затемнением и окнами
        g.pose().pushPose();
        g.pose().translate(0, 0, Z_TOP_BUTTONS);
        for (int slotX : new int[]{WALLET_X, BELT_X, BACKPACK_X}) {
            UiDraw.plusButton(g, slotX + 10, BOTTOM_Y + 1);
        }
        g.pose().popPose();
    }

    /** Окно рюкзака 176×201 (ТЗ 3.1). */
    private void renderBackpack(GuiGraphics g) {
        int px = BACKPACK_WIN_X, py = BACKPACK_WIN_Y;
        UiDraw.panel(g, px, py, BACKPACK_W, BACKPACK_H);
        g.drawString(font, Component.translatable("gui.trailcraft.backpack"), px + 7, py + 6, UiDraw.LABEL, false);
        boolean present = menu.backpack().isPresent();
        for (int i = 0; i < 7; i++) {
            int rowY = py + 20 + 21 * i;
            drawSlotFrame(g, px + 7, rowY, !present);
            boolean bar = BAR_COLORS[i] != 0;
            g.drawString(font, Component.translatable("gui.trailcraft.backpack." + BACKPACK_ROWS[i]), px + 29, rowY + (bar ? 1 : 5), UiDraw.LABEL, false);
            if (bar) {
                // Первая шкала — энергия (электричество) аккумулятора, вторая — мана.
                // TODO: заряд аккумулятора и мана планшета, когда появятся эти предметы
                float fill = 0;
                int barW = BACKPACK_W - 38;
                UiDraw.frame(g, px + 29, rowY + 10, barW, 6, 0xFF373737, 0xFFFFFFFF, 0xFF555555);
                g.fill(px + 30, rowY + 11, px + 30 + Math.round((barW - 2) * fill), rowY + 15, BAR_COLORS[i]);
            }
        }
        g.drawString(font, Component.translatable("gui.trailcraft.backpack.pockets"), px + 7, py + 166, UiDraw.LABEL, false);
        for (int k = 0; k < menu.visiblePockets(); k++) {
            drawSlotFrame(g, px + 7 + 18 * k, py + 176, !present);
        }
        renderPopupItems(g);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, boolean locked) {
        if (locked) {
            UiDraw.lockedSlot(g, x, y);
        } else {
            UiDraw.slot(g, x, y);
        }
    }

    /** Предметы в слотах окна рисуются заново поверх затемнения. */
    private void renderPopupItems(GuiGraphics g) {
        g.pose().pushPose();
        g.pose().translate(0, 0, Z_POPUP_ITEMS);
        for (Slot slot : menu.slots) {
            if (slot instanceof MenuSlot s && s.group() == GROUP_BACKPACK && slot.isActive()) {
                ItemStack stack = slot.getItem();
                g.renderItem(stack, slot.x, slot.y);
                g.renderItemDecorations(font, stack, slot.x, slot.y);
                if (slot == hoveredSlot) {
                    g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF);
                }
            }
        }
        g.pose().popPose();
    }

    /** Окно журнала смертей: дата, игровая дата, координаты и что с рюкзаком. Колесо мыши — прокрутка. */
    private void renderDeaths(GuiGraphics g) {
        int px = DEATHS_X, py = DEATHS_Y;
        UiDraw.panel(g, px, py, DEATHS_W, DEATHS_H);
        g.drawString(font, Component.translatable("gui.trailcraft.deaths"), px + 7, py + 6, UiDraw.LABEL, false);
        Component total = Component.translatable("gui.trailcraft.deaths.total", deathsTotal);
        g.drawString(font, total, px + DEATHS_W - 7 - font.width(total), py + 6, UiDraw.LABEL, false);
        if (deaths.isEmpty()) {
            g.drawString(font, Component.translatable("gui.trailcraft.deaths.empty"), px + 7, py + 22, 0xFF606060, false);
            return;
        }
        int rows = (DEATHS_H - 22) / DEATH_ROW_H;
        for (int i = 0; i < rows && deathsScroll + i < deaths.size(); i++) {
            int index = deathsScroll + i;
            DeathRecord death = deaths.get(index);
            int ry = py + 20 + i * DEATH_ROW_H;
            UiDraw.frame(g, px + 6, ry - 1, DEATHS_W - 12, DEATH_ROW_H - 2, 0xFF8B8B8B, 0xFFFFFFFF, 0xFFB8B8B8);
            String dimension = death.dimension().equals("minecraft:overworld") ? "" : "  " + death.dimension().replace("minecraft:", "");
            g.drawString(font, "№" + (deathsTotal - index) + "  " + DATE.format(Instant.ofEpochMilli(death.time())) + dimension,
                    px + 9, ry + 1, UiDraw.LABEL, false);
            Calendar date = Calendar.of(death.dayTime(), ServerConfig.daysPerSeason());
            g.drawString(font, Component.translatable("gui.trailcraft.deaths.date", date.year(),
                    Component.translatable(date.season().translationKey()), date.dayOfSeason()), px + 9, ry + 10, 0xFF606060, false);
            DeathRecord.BackpackStatus status = death.status(deathsNow);
            g.drawString(font, Component.translatable("gui.trailcraft.deaths.place", death.pos().getX(), death.pos().getY(), death.pos().getZ(),
                    Component.translatable(status.translationKey())), px + 9, ry + 19, UiDraw.LABEL, false);
        }
    }

    // === Ввод ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int corner = CornerButtons.hovered(cornerButtons, mouseX, mouseY);
        if (corner >= 0) {
            cornerButtons.get(corner).action().run();
            return true;
        }
        if (voiceOpen) {
            if (!voiceWindow.mouseClicked(mouseX, mouseY)) {
                voiceOpen = false;
            }
            return true;
        }
        if (menu.popup != Popup.NONE) {
            if (isOver(mouseX, mouseY, BACKPACK_X + 10, BOTTOM_Y + 1, 7, 7) && menu.popup == Popup.BACKPACK) {
                openPopup(Popup.NONE);
                return true;
            }
            // Клик по видимому слоту инвентаря проходит в слот (чтобы перекладывать вещи в окно и из него),
            // клик по пустому месту вне окна закрывает окно.
            if (!overPopup(mouseX, mouseY) && !overActiveSlot(mouseX, mouseY)) {
                openPopup(Popup.NONE);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            for (int i = 0; i < 4; i++) {
                if (isOver(mouseX, mouseY, COSMETIC_X + 12, EQUIP_Y + i * 18 + 1, 5, 5)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
            if (isOver(mouseX, mouseY, BACKPACK_X + 10, BOTTOM_Y + 1, 7, 7)) {
                openPopup(Popup.BACKPACK);
                return true;
            }
            // TODO (шаг 3): окна кошелька и пояса
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && voiceOpen) {
            voiceOpen = false;
            return true;
        }
        if (keyCode == 256 && menu.popup != Popup.NONE) {
            openPopup(Popup.NONE);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (voiceOpen) {
            return voiceWindow.mouseScrolled(mouseX, mouseY, scrollY);
        }
        if (menu.popup == Popup.DEATHS) {
            int rows = (DEATHS_H - 22) / DEATH_ROW_H;
            deathsScroll = Math.max(0, Math.min(deathsScroll - (int) Math.signum(scrollY), deaths.size() - rows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean overActiveSlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /** Попадание в прямоугольник, заданный относительно панели. */
    private boolean isOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        return rx >= x && rx < x + w && ry >= y && ry < y + h;
    }

    /** Клик по головам голосового чата или по окну не считается кликом мимо (иначе предмет с курсора выбросится). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        boolean onHeads = mouseX >= left - HEADS_GAP - HEADS_W && mouseX < left
                && mouseY >= top + HEADS_Y - 1 && mouseY < top + HEADS_Y + VoiceClient.HEADS * VoiceHeads.STEP;
        return !onHeads && !overPopup(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }
}

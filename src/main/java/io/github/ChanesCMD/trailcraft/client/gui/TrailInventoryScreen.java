package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.calendar.CalendarNotes;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceClient;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceHeads;
import io.github.ChanesCMD.trailcraft.inventory.DeathLogPayloads;
import io.github.ChanesCMD.trailcraft.inventory.DeathRecord;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.MenuSlot;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.Popup;
import io.github.ChanesCMD.trailcraft.item.CoinItem;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import io.github.ChanesCMD.trailcraft.water.Water;
import io.github.ChanesCMD.trailcraft.water.WaterEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.*;

/**
 * Экран инвентаря (ТЗ, часть 1, разделы 1–3). Макеты: docs/ui/01_inventory_default.png, 02_inventory_warmth.png, 04_backpack.png.
 * Слева от панели — головы голосового чата (только когда в нём хотя бы двое), в левом верхнем углу экрана — 5 кнопок.
 * Окна (рюкзак, кошелёк, пояс, календарь, журнал смертей, голосовой чат) рисуются поверх инвентаря с лёгким затемнением; закрываются Esc,
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
    /** Окно календаря 140×158 (ТЗ 3.4): под кнопкой, правый край по кнопке; ячейки дней 20×18 с (10,51), по 6 в ряд. */
    private static final int CAL_W = 140, CAL_H = 158, CAL_CELL_W = 20, CAL_CELL_H = 18, CAL_GRID_X = 10, CAL_GRID_Y = 51;
    private static final int CAL_CLOSE_X = 122, CAL_CLOSE_Y = 5, CAL_NOTE_X = 10, CAL_NOTE_Y = 137;
    /** Стрелки листания сезонов 10×12 слева от кнопки закрытия; шапка (клик — вернуться к сегодня). */
    private static final int CAL_PREV_X = 96, CAL_NEXT_X = 108, CAL_ARROW_W = 10, CAL_HEADER_X = 8, CAL_HEADER_Y = 20;
    /** Кнопка «Выпить» в окне пояса (относительно окна). */
    private static final int DRINK_X = 7, DRINK_Y = 42, DRINK_W = 112, DRINK_H = 14;
    private static final int[] BAR_COLORS = {0xFFE8C33A, 0, 0xFF4A7BE0, 0, 0, 0, 0};
    private static final String[] BACKPACK_ROWS = {"battery", "solar", "mana", "sleeping", "workbench", "atlas", "calendar"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());
    /**
     * Слои по глубине: предметы в слотах ≈150–200, затемнение и окна выше, предметы в окнах ≈380–430 (как предмет
     * на курсоре). Кнопки окон (+) лежат выше затемнения и окон: ими же окно и закрывается. Подсказка — выше всего.
     */
    private static final int Z_BUTTONS = 210, Z_DIM = 220, Z_POPUP = 230, Z_POPUP_ITEMS = 0, Z_TOP_BUTTONS = 300, Z_TOOLTIP = 300;

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
    /** Окно календаря: положение относительно панели (прижимается к краю экрана), заметки, выбранный день. */
    private int calX, calY;
    private EditBox noteBox;
    private Map<String, String> notes = new HashMap<>();
    private int selectedDay;
    private String noteKey;
    /** На сколько сезонов показанный в календаре сезон впереди (или позади) текущего. */
    private int seasonOffset;

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
        // Окно рюкзака над панелью должно влезать по высоте (GUI scale 4 на 1080p: экран 480×270)
        topPos = Math.min(Math.max((height - HEIGHT) / 2 - RAISE, 4 - BACKPACK_WIN_Y), Math.max(4, height - HEIGHT - 4));
        cornerButtons = CornerButtons.create(this, this::toggleVoice);
        voiceWindow = new VoiceChatWindow(font);
        for (int i = 0; i < cornerButtons.size(); i++) {
            if (cornerButtons.get(i).id().equals("voice")) {
                voiceWindow.place(CornerButtons.x(i), width, height);
            }
        }

        calendarButton = addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.calendar"), b -> toggleCalendar())
                .bounds(leftPos + CALENDAR_X, topPos + CALENDAR_Y, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.deaths"), b -> toggleDeaths())
                .bounds(leftPos + CALENDAR_X, topPos + DEATHS_BUTTON_Y, BUTTON_W, BUTTON_H).build());
        updateCalendarButton();

        // Поле заметки рисуется вместе с окном календаря (поверх затемнения), поэтому добавлено без отрисовки
        placeCalendar();
        noteBox = new EditBox(font, leftPos + calX + CAL_NOTE_X, topPos + calY + CAL_NOTE_Y, 120, 15, Component.translatable("gui.trailcraft.calendar.note"));
        noteBox.setMaxLength(CalendarNotes.MAX_LENGTH);
        noteBox.setVisible(menu.popup == Popup.CALENDAR);
        if (noteKey != null) {
            noteBox.setValue(notes.getOrDefault(noteKey, ""));
        }
        addWidget(noteBox);
        if (menu.popup == Popup.CALENDAR) {
            setPopupRect(calX, calY, CAL_W, CAL_H);
        }
    }

    @Override
    public void removed() {
        flushNote();
        super.removed();
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
        if (menu.popup == Popup.CALENDAR && popup != Popup.CALENDAR) {
            flushNote();
            noteBox.setFocused(false);
            noteBox.setVisible(false);
            setFocused(null);
        }
        menu.popup = popup;
        switch (popup) {
            case BACKPACK -> setPopupRect(BACKPACK_WIN_X, BACKPACK_WIN_Y, BACKPACK_W, BACKPACK_H);
            case WALLET -> setPopupRect(WALLET_WIN_X, WALLET_WIN_Y, WALLET_W, WALLET_H);
            case BELT -> setPopupRect(BELT_WIN_X, BELT_WIN_Y, BELT_W, BELT_H);
            case CALENDAR -> setPopupRect(calX, calY, CAL_W, CAL_H);
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

    /** Окно календаря под кнопкой; если не влезает по высоте или ширине — прижимается к краю экрана. */
    private void placeCalendar() {
        calX = Math.max(4 - leftPos, Math.min(CALENDAR_X + BUTTON_W - CAL_W, width - 4 - CAL_W - leftPos));
        calY = Math.max(4 - topPos, Math.min(CALENDAR_Y + BUTTON_H + 2, height - 4 - CAL_H - topPos));
    }

    private void toggleCalendar() {
        if (menu.popup == Popup.CALENDAR) {
            openPopup(Popup.NONE);
            return;
        }
        openPopup(Popup.CALENDAR);
        noteBox.setVisible(true);
        seasonOffset = 0;
        selectDay(today().dayOfSeason());
        PacketDistributor.sendToServer(CalendarNotes.Request.INSTANCE);
    }

    private Calendar today() {
        return Calendar.of(minecraft.level.getDayTime(), ServerConfig.daysPerSeason());
    }

    /** Номер сезона от начала календаря (0 — весна START_YEAR). */
    private static long seasonIndex(Calendar calendar) {
        return (calendar.year() - Calendar.START_YEAR) * 4 + calendar.season().ordinal();
    }

    /** Показанный сезон: номер от начала календаря. */
    private long shownSeason() {
        return Math.max(0, seasonIndex(today()) + seasonOffset);
    }

    /** Листает календарь на delta сезонов (не раньше первого); 0 — вернуться к текущему. */
    private void turnSeason(int delta) {
        long current = seasonIndex(today());
        int offset = delta == 0 ? 0 : (int) Math.max(-current, seasonOffset + delta);
        if (offset != seasonOffset) {
            seasonOffset = offset;
            selectDay(offset == 0 ? today().dayOfSeason() : 1);
        }
    }

    /** Выбирает день показанного сезона: заметка прежнего дня сохраняется, в поле — заметка нового. */
    private void selectDay(int day) {
        flushNote();
        selectedDay = day;
        long shown = shownSeason();
        noteKey = CalendarNotes.key(Calendar.START_YEAR + shown / 4, (int) (shown % 4), day);
        noteBox.setValue(notes.getOrDefault(noteKey, ""));
    }

    /** Отправляет заметку на сервер, если она изменилась. */
    private void flushNote() {
        if (noteKey == null || noteBox == null) {
            return;
        }
        String text = noteBox.getValue().strip();
        if (!text.equals(notes.getOrDefault(noteKey, ""))) {
            if (text.isEmpty()) {
                notes.remove(noteKey);
            } else {
                notes.put(noteKey, text);
            }
            PacketDistributor.sendToServer(new CalendarNotes.Update(noteKey, text));
        }
    }

    public void showCalendarNotes(Map<String, String> notes) {
        this.notes = new HashMap<>(notes);
        if (noteKey != null && noteBox.getValue().isEmpty()) {
            noteBox.setValue(this.notes.getOrDefault(noteKey, ""));
        }
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
            // Подсказка рисуется на z≈400; поднимаем её над предметами в окнах (у них количество на z≈430)
            g.pose().pushPose();
            g.pose().translate(0, 0, Z_TOOLTIP);
            renderTooltip(g, mouseX, mouseY);
            g.pose().popPose();
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
            switch (menu.popup) {
                case BACKPACK -> renderBackpack(g);
                case WALLET -> renderWallet(g);
                case BELT -> renderBelt(g, mouseX, mouseY);
                case CALENDAR -> renderCalendar(g, mouseX, mouseY);
                default -> renderDeaths(g);
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

    /** Окно кошелька 86×64 (ТЗ 3.3): 8 ячеек 4×2, по виду монеты в каждой. */
    private void renderWallet(GuiGraphics g) {
        int px = WALLET_WIN_X, py = WALLET_WIN_Y;
        UiDraw.panel(g, px, py, WALLET_W, WALLET_H);
        g.drawString(font, Component.translatable("gui.trailcraft.wallet"), px + 7, py + 6, UiDraw.LABEL, false);
        boolean present = menu.wallet().isPresent();
        for (int k = 0; k < 8; k++) {
            drawSlotFrame(g, px + 7 + 18 * (k % 4), py + 20 + 18 * (k / 4), !present);
        }
        renderPopupItems(g);
    }

    /** Окно пояса: слот фляги, запас воды в ней и кнопка «Выпить». */
    private void renderBelt(GuiGraphics g, int mouseX, int mouseY) {
        int px = BELT_WIN_X, py = BELT_WIN_Y;
        UiDraw.panel(g, px, py, BELT_W, BELT_H);
        g.drawString(font, Component.translatable("gui.trailcraft.belt"), px + 7, py + 6, UiDraw.LABEL, false);
        drawSlotFrame(g, px + 7, py + 20, !menu.belt().isPresent());
        ItemStack flask = menu.belt().flask();
        int water = FlaskItem.water(flask);
        Component label = flask.isEmpty() ? Component.translatable("gui.trailcraft.belt.no_flask")
                : Component.translatable("gui.trailcraft.belt.flask", water, Water.FLASK_SIPS);
        g.drawString(font, label, px + 29, py + 21, UiDraw.LABEL, false);
        UiDraw.frame(g, px + 29, py + 31, 90, 6, 0xFF373737, 0xFFFFFFFF, 0xFF555555);
        g.fill(px + 30, py + 32, px + 30 + Math.round(88f * water / Water.FLASK_SIPS), py + 36, 0xFF3F76E4);

        boolean active = canDrinkFromFlask();
        boolean hover = active && isOver(mouseX, mouseY, px + DRINK_X, py + DRINK_Y, DRINK_W, DRINK_H);
        UiDraw.frame(g, px + DRINK_X, py + DRINK_Y, DRINK_W, DRINK_H, 0xFF000000, active ? 0xFFFFFFFF : 0xFF9E9E9E,
                hover ? 0xFFB0B0D8 : active ? 0xFFA8A8A8 : 0xFF6E6E6E);
        Component drink = Component.translatable("gui.trailcraft.belt.drink");
        g.drawString(font, drink, px + DRINK_X + (DRINK_W - font.width(drink)) / 2, py + DRINK_Y + 3,
                active ? 0xFFFFFFFF : 0xFFA0A0A0, active);
        renderPopupItems(g);
    }

    private boolean canDrinkFromFlask() {
        return FlaskItem.water(menu.belt().flask()) > 0 && WaterEvents.get(playerInventory.player) < Water.MAX;
    }

    /**
     * Окно календаря 140×158 (ТЗ 3.4): шапка с годом и сезоном показанной страницы, сегодняшним днём и временем;
     * сетка дней сезона (6 в ряд): прошедшие темнее, текущий зелёный с белой рамкой, выбранный в чёрной рамке,
     * с заметкой — жёлтая точка; внизу заметка к выбранному дню. Стрелки и колесо листают сезоны, клик по шапке —
     * обратно к сегодня.
     */
    private void renderCalendar(GuiGraphics g, int mouseX, int mouseY) {
        int px = calX, py = calY;
        Calendar now = today();
        long current = seasonIndex(now), shown = shownSeason();
        long shownYear = Calendar.START_YEAR + shown / 4;
        int shownSeasonOrdinal = (int) (shown % 4);
        UiDraw.panel(g, px, py, CAL_W, CAL_H);
        g.drawString(font, Component.translatable("gui.trailcraft.calendar"), px + 7, py + 6, UiDraw.LABEL, false);
        arrowButton(g, px + CAL_PREV_X, py + CAL_CLOSE_Y, mouseX, mouseY, -1, shown > 0);
        arrowButton(g, px + CAL_NEXT_X, py + CAL_CLOSE_Y, mouseX, mouseY, 1, true);
        // Кнопка закрытия 12×12 с крестиком
        int cx = px + CAL_CLOSE_X, cy = py + CAL_CLOSE_Y;
        boolean closeHover = isOver(mouseX, mouseY, cx, cy, 12, 12);
        UiDraw.frame(g, cx, cy, 12, 12, 0xFF000000, 0xFFFFFFFF, closeHover ? 0xFFB0B0D8 : 0xFFA8A8A8);
        for (int i = 0; i < 6; i++) {
            g.fill(cx + 3 + i, cy + 3 + i, cx + 4 + i, cy + 4 + i, UiDraw.LABEL);
            g.fill(cx + 8 - i, cy + 3 + i, cx + 9 - i, cy + 4 + i, UiDraw.LABEL);
        }

        UiDraw.frame(g, px + CAL_HEADER_X, py + CAL_HEADER_Y, 124, 26, 0xFF373737, 0xFFFFFFFF, shown == current ? 0xFFB8B8B8 : 0xFFC8C0A0);
        Component season = Component.translatable(Calendar.Season.values()[shownSeasonOrdinal].translationKey());
        g.drawString(font, Component.translatable("gui.trailcraft.calendar.year", shownYear), px + 12, py + 23, UiDraw.LABEL, false);
        g.drawString(font, season, px + 128 - font.width(season), py + 23, UiDraw.LABEL, false);
        g.drawString(font, Component.translatable("gui.trailcraft.calendar.day", now.dayOfSeason()), px + 12, py + 34, UiDraw.LABEL, false);
        g.drawString(font, now.time(), px + 128 - font.width(now.time()), py + 34, UiDraw.LABEL, false);

        for (int day = 1; day <= now.daysPerSeason(); day++) {
            int x = px + CAL_GRID_X + CAL_CELL_W * ((day - 1) % 6), y = py + CAL_GRID_Y + CAL_CELL_H * ((day - 1) / 6);
            boolean today = shown == current && day == now.dayOfSeason();
            boolean past = shown < current || shown == current && day < now.dayOfSeason();
            int fill = today ? UiDraw.GREEN : past ? 0xFF7A7A7A : 0xFFB8B8B8;
            int border = today ? 0xFFFFFFFF : day == selectedDay ? 0xFF000000 : 0xFF8B8B8B;
            g.fill(x, y, x + CAL_CELL_W, y + CAL_CELL_H, border);
            g.fill(x + 1, y + 1, x + CAL_CELL_W - 1, y + CAL_CELL_H - 1, fill);
            if (day == selectedDay && !today) {
                g.fill(x + 1, y + 1, x + CAL_CELL_W - 1, y + 2, 0xFF000000);
                g.fill(x + 1, y + CAL_CELL_H - 2, x + CAL_CELL_W - 1, y + CAL_CELL_H - 1, 0xFF000000);
            }
            String number = String.valueOf(day);
            g.drawString(font, number, x + (CAL_CELL_W - font.width(number) + 1) / 2, y + 5, today ? 0xFFFFFFFF : UiDraw.LABEL, today);
            if (!notes.getOrDefault(CalendarNotes.key(shownYear, shownSeasonOrdinal, day), "").isEmpty()) {
                g.fill(x + CAL_CELL_W - 5, y + 2, x + CAL_CELL_W - 2, y + 5, 0xFFF2D23A);
            }
        }

        g.drawString(font, Component.translatable("gui.trailcraft.calendar.note_for", selectedDay), px + CAL_NOTE_X, py + CAL_NOTE_Y - 10,
                UiDraw.LABEL, false);
        // Поле ввода в координатах экрана
        g.pose().pushPose();
        g.pose().translate(-leftPos, -topPos, 0);
        noteBox.setPosition(leftPos + px + CAL_NOTE_X, topPos + py + CAL_NOTE_Y);
        noteBox.render(g, mouseX, mouseY, 0);
        g.pose().popPose();
    }

    /** Стрелка листания 10×12: direction −1 — назад, 1 — вперёд. */
    private void arrowButton(GuiGraphics g, int x, int y, int mouseX, int mouseY, int direction, boolean active) {
        boolean hover = active && isOver(mouseX, mouseY, x, y, CAL_ARROW_W, 12);
        UiDraw.frame(g, x, y, CAL_ARROW_W, 12, 0xFF000000, active ? 0xFFFFFFFF : 0xFF9E9E9E,
                hover ? 0xFFB0B0D8 : active ? 0xFFA8A8A8 : 0xFF6E6E6E);
        int color = active ? UiDraw.LABEL : 0xFF8B8B8B;
        for (int i = 0; i < 4; i++) {
            int column = direction < 0 ? x + 3 + i : x + 6 - i;
            g.fill(column, y + 6 - i, column + 1, y + 7 + i, color);
        }
    }

    /** День под курсором в окне календаря или 0. */
    private int calendarDayAt(double mouseX, double mouseY) {
        int days = today().daysPerSeason();
        for (int day = 1; day <= days; day++) {
            int x = calX + CAL_GRID_X + CAL_CELL_W * ((day - 1) % 6), y = calY + CAL_GRID_Y + CAL_CELL_H * ((day - 1) / 6);
            if (isOver(mouseX, mouseY, x, y, CAL_CELL_W, CAL_CELL_H)) {
                return day;
            }
        }
        return 0;
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
            if (slot instanceof MenuSlot s && s.group() == popupGroup() && slot.isActive()) {
                ItemStack stack = slot.getItem();
                g.renderItem(stack, slot.x, slot.y);
                g.renderItemDecorations(font, stack, slot.x, slot.y);
                if (slot == hoveredSlot) {
                    // Подсветка поверх предмета (он рисуется на +150)
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 160);
                    g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF);
                    g.pose().popPose();
                }
            }
        }
        g.pose().popPose();
    }

    /** Группа слотов открытого окна. */
    private int popupGroup() {
        return switch (menu.popup) {
            case BACKPACK -> GROUP_BACKPACK;
            case WALLET -> GROUP_WALLET;
            case BELT -> GROUP_BELT;
            default -> -1;
        };
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
        // Кнопки окон на кошельке, поясе и рюкзаке лежат поверх всего и работают всегда: открыть, переключить, закрыть
        Popup plus = button == 0 ? plusButtonAt(mouseX, mouseY) : null;
        if (plus != null) {
            openPopup(menu.popup == plus ? Popup.NONE : plus);
            return true;
        }
        if (menu.popup == Popup.BELT && button == 0 && canDrinkFromFlask()
                && isOver(mouseX, mouseY, BELT_WIN_X + DRINK_X, BELT_WIN_Y + DRINK_Y, DRINK_W, DRINK_H)) {
            PacketDistributor.sendToServer(Water.FlaskDrinkPayload.INSTANCE);
            return true;
        }
        if (menu.popup == Popup.CALENDAR && overPopup(mouseX, mouseY)) {
            boolean onNote = noteBox.isMouseOver(mouseX, mouseY);
            noteBox.setFocused(onNote);
            setFocused(onNote ? noteBox : null);
            if (onNote) {
                noteBox.mouseClicked(mouseX, mouseY, button);
            } else if (isOver(mouseX, mouseY, calX + CAL_CLOSE_X, calY + CAL_CLOSE_Y, 12, 12)) {
                openPopup(Popup.NONE);
            } else if (isOver(mouseX, mouseY, calX + CAL_PREV_X, calY + CAL_CLOSE_Y, CAL_ARROW_W, 12)) {
                turnSeason(-1);
            } else if (isOver(mouseX, mouseY, calX + CAL_NEXT_X, calY + CAL_CLOSE_Y, CAL_ARROW_W, 12)) {
                turnSeason(1);
            } else if (isOver(mouseX, mouseY, calX + CAL_HEADER_X, calY + CAL_HEADER_Y, 124, 26)) {
                turnSeason(0);
            } else {
                int day = calendarDayAt(mouseX, mouseY);
                if (day > 0) {
                    selectDay(day);
                }
            }
            return true;
        }
        if (menu.popup != Popup.NONE) {
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Монета с курсора кладётся в ячейку своего вида, в какую бы ячейку кошелька ни кликнули:
     * иначе клик по занятой ячейке поменял бы монеты местами.
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null && type == ClickType.PICKUP && slotId >= WALLET_CONTENT_START && slotId < WALLET_CONTENT_START + CoinItem.TIERS
                && menu.getCarried().getItem() instanceof CoinItem coin) {
            slotId = WALLET_CONTENT_START + coin.tier();
            slot = menu.slots.get(slotId);
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    /** Кнопка окна (+) под курсором: кошелёк, пояс или рюкзак. */
    private Popup plusButtonAt(double mouseX, double mouseY) {
        if (isOver(mouseX, mouseY, WALLET_X + 10, BOTTOM_Y + 1, 7, 7)) return Popup.WALLET;
        if (isOver(mouseX, mouseY, BELT_X + 10, BOTTOM_Y + 1, 7, 7)) return Popup.BELT;
        if (isOver(mouseX, mouseY, BACKPACK_X + 10, BOTTOM_Y + 1, 7, 7)) return Popup.BACKPACK;
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (menu.popup == Popup.CALENDAR && noteBox.isFocused() && keyCode != 256) {
            // Пока пишется заметка, клавиши (E, цифры хотбара…) идут в поле, Enter сохраняет
            if (keyCode == 257 || keyCode == 335) {
                flushNote();
                noteBox.setFocused(false);
                setFocused(null);
                return true;
            }
            noteBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
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
    public boolean charTyped(char codePoint, int modifiers) {
        if (menu.popup == Popup.CALENDAR && noteBox.isFocused()) {
            return noteBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (voiceOpen) {
            return voiceWindow.mouseScrolled(mouseX, mouseY, scrollY);
        }
        if (menu.popup == Popup.CALENDAR) {
            if (scrollY != 0) {
                turnSeason(scrollY > 0 ? -1 : 1);
            }
            return true;
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

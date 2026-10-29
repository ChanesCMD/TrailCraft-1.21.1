package io.github.ChanesCMD.trailcraft.client.gui;

import com.google.common.collect.Lists;
import io.github.ChanesCMD.trailcraft.inventory.OpenInventoryPayload;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Своё креативное окно вместо ванильного: того же размера, что инвентарь TrailCraft (TrailInventoryMenu.WIDTH × HEIGHT),
 * и на том же месте, вкладки сверху одним рядом (CreativeTabs). Сетка предметов COLS × ROWS с прокруткой, хотбар — там
 * же, где в инвентаре. Клики — как в ванильном креативе (CreativeModeInventoryScreen.slotClicked): из сетки берут
 * предмет, в сетку — выбрасывают; хотбар меняется на клиенте и отправляется серверу (handleCreativeModeItemAdd).
 * Вкладка «Инвентарь» — инвентарь TrailCraft (сервер открывает его по OpenInventoryPayload.fromCreative).
 */
public class TrailCreativeScreen extends AbstractContainerScreen<TrailCreativeScreen.PickerMenu> {
    public static final int COLS = 20, ROWS = 7, GRID_X = 9, GRID_Y = 18;
    private static final int SCROLL_X = 372, SCROLL_W = 12, SCROLL_H = ROWS * 18, SEARCH_X = 248, SEARCH_W = 120;
    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    /** Последняя выбранная вкладка: на ней креатив и откроется. */
    private static CreativeModeTab lastTab;

    private final boolean operatorTab;
    private CreativeModeTab tab;
    private CreativeTabs tabs;
    private EditBox searchBox;
    private CreativeInventoryListener listener;
    private float scrollOffs;
    private boolean scrolling, ignoreTextInput;

    public TrailCreativeScreen(LocalPlayer player, @Nullable CreativeModeTab tab) {
        super(new PickerMenu(player), player.getInventory(), CommonComponents.EMPTY);
        player.containerMenu = menu;
        imageWidth = TrailInventoryMenu.WIDTH;
        imageHeight = TrailInventoryMenu.HEIGHT;
        Minecraft mc = Minecraft.getInstance();
        operatorTab = mc.options.operatorItemsTab().get();
        rebuildTabs(player);
        this.tab = tab != null && tab.shouldDisplay() ? tab : CreativeModeTabs.getDefaultTab();
    }

    /** Последняя вкладка креатива (null — ещё не выбирали). */
    public static CreativeModeTab lastTab() {
        return lastTab;
    }

    /**
     * Открыть креатив на вкладке (из инвентаря TrailCraft: сначала закрыть его меню на сервере). Не closeContainer:
     * тот ставит пустой экран, мышь захватывается, и новое окно открывается с курсором в центре экрана.
     */
    public static void open(CreativeModeTab tab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        mc.setScreen(new TrailCreativeScreen(mc.player, tab));
    }

    private boolean rebuildTabs(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        var registries = player.level().registryAccess();
        if (!CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(),
                player.canUseGameMasterBlocks() && operatorTab, registries)) {
            return false;
        }
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            SessionSearchTrees trees = connection.searchTrees();
            CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar).forEach(t -> {
                List<ItemStack> list = List.copyOf(t.getDisplayItems());
                trees.updateCreativeTooltips(registries, list, CreativeModeTabSearchRegistry.getNameSearchKey(t));
                trees.updateCreativeTags(list, CreativeModeTabSearchRegistry.getTagSearchKey(t));
            });
        }
        return true;
    }

    @Override
    protected void init() {
        super.init();
        // на том же месте, что инвентарь TrailCraft: вкладки при переключении не прыгают
        leftPos = TrailInventoryScreen.left(width);
        topPos = TrailInventoryScreen.top(height);
        tabs = new CreativeTabs(imageWidth, tab, this::selectTab);
        tabs.addButtons(leftPos, topPos, imageWidth, this::addRenderableWidget);
        String search = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(font, leftPos + SEARCH_X + 2, topPos + 6, SEARCH_W - 4, 9, Component.translatable("itemGroup.search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setValue(search);
        addWidget(searchBox);
        if (listener != null) {
            minecraft.player.inventoryMenu.removeSlotListener(listener);
        }
        listener = new CreativeInventoryListener(minecraft);
        minecraft.player.inventoryMenu.addSlotListener(listener);
        showTab(tab, false);
    }

    @Override
    public void removed() {
        super.removed();
        if (minecraft.player != null && listener != null) {
            minecraft.player.inventoryMenu.removeSlotListener(listener);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!minecraft.gameMode.hasInfiniteItems()) {
            minecraft.player.closeContainer();
            return;
        }
        if (rebuildTabs(minecraft.player)) {
            // содержимое вкладок пересобрано (права, флаги) — показать заново, сохранив прокрутку
            int row = menu.rowForScroll(scrollOffs);
            showTab(tab.shouldDisplay() ? tab : CreativeModeTabs.getDefaultTab(), false);
            scrollOffs = menu.scrollForRow(row);
            menu.scrollTo(scrollOffs);
        }
    }

    // === Вкладки ===

    private void selectTab(CreativeModeTab next) {
        if (next.getType() == CreativeModeTab.Type.INVENTORY) {
            // инвентарь TrailCraft открывает сервер; предмет с курсора уходит туда же
            lastTab = next;
            ItemStack carried = menu.getCarried().copy();
            menu.setCarried(ItemStack.EMPTY);
            PacketDistributor.sendToServer(OpenInventoryPayload.fromCreative(carried));
            return;
        }
        showTab(next, true);
        rebuildWidgets();
    }

    private void showTab(CreativeModeTab next, boolean clearSearch) {
        boolean changed = next != tab;
        tab = next;
        lastTab = next;
        menu.items.clear();
        clearDraggingState();
        if (tab.hasSearchBar()) {
            searchBox.setVisible(true);
            searchBox.setCanLoseFocus(false);
            searchBox.setFocused(true);
            if (changed && clearSearch) {
                searchBox.setValue("");
            }
            refreshSearchResults();
        } else {
            searchBox.setVisible(false);
            searchBox.setCanLoseFocus(true);
            searchBox.setFocused(false);
            searchBox.setValue("");
            menu.items.addAll(tab.getDisplayItems());
        }
        scrollOffs = 0;
        menu.scrollTo(0);
    }

    private void refreshSearchResults() {
        menu.items.clear();
        String query = searchBox.getValue();
        if (query.isEmpty()) {
            menu.items.addAll(tab.getDisplayItems());
        } else if (minecraft.getConnection() != null) {
            SessionSearchTrees trees = minecraft.getConnection().searchTrees();
            SearchTree<ItemStack> tree;
            if (query.startsWith("#")) {
                query = query.substring(1);
                tree = trees.creativeTagSearch(CreativeModeTabSearchRegistry.getTagSearchKey(tab));
            } else {
                tree = trees.creativeNameSearch(CreativeModeTabSearchRegistry.getNameSearchKey(tab));
            }
            menu.items.addAll(tree.search(query.toLowerCase(Locale.ROOT)));
        }
        scrollOffs = 0;
        menu.scrollTo(0);
    }

    /** Где вкладки (чтобы JEI их не закрывал). */
    public Rect2i tabsArea() {
        return tabs.area(leftPos, topPos, imageWidth);
    }

    // === Отрисовка ===

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        searchBox.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        TrashSlot.renderTooltip(g, font, leftPos, topPos, mouseX, mouseY, menu);
        if (menu.getCarried().isEmpty()) {
            tabs.renderTooltip(g, font, leftPos, topPos, imageWidth, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        tabs.renderBehind(g, x, y, imageWidth);
        UiDraw.panel(g, x, y, imageWidth, imageHeight);
        tabs.renderFront(g, font, x, y, imageWidth);
        for (Slot slot : menu.slots) {
            UiDraw.slot(g, x + slot.x - 1, y + slot.y - 1);
        }
        if (tab.hasSearchBar()) {
            UiDraw.frame(g, x + SEARCH_X, y + 4, SEARCH_W, 12, 0xFF373737, 0xFFFFFFFF, 0xFF000000);
        }
        // полоса прокрутки
        int sx = x + SCROLL_X, sy = y + GRID_Y - 1;
        UiDraw.frame(g, sx - 1, sy, SCROLL_W + 2, SCROLL_H + 1, 0xFF373737, 0xFFFFFFFF, 0xFF8B8B8B);
        g.blitSprite(menu.canScroll() ? SCROLLER : SCROLLER_DISABLED, sx, sy + 1 + (int) ((SCROLL_H - 16) * scrollOffs),
                SCROLL_W, 15);
        TrashSlot.render(g, x, y, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (tab.showTitle()) {
            g.drawString(font, tab.getDisplayName(), 8, 6, UiDraw.LABEL, false);
        }
    }

    /** Подсказка предмета из сетки — с названиями вкладок, где он есть, как в ванильном креативе. */
    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack stack) {
        boolean grid = hoveredSlot != null && hoveredSlot.container == menu.grid;
        TooltipFlag.Default flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player,
                ClientTooltipFlag.of(grid ? flag.asCreative() : flag));
        if (grid && tab.getType() == CreativeModeTab.Type.CATEGORY) {
            return lines;
        }
        List<Component> out = Lists.newArrayList(lines);
        int i = 1;
        for (CreativeModeTab t : CreativeModeTabs.tabs()) {
            if (!t.hasSearchBar() && t.contains(stack)) {
                out.add(i++, t.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
        }
        return out;
    }

    // === Ввод ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && tabs.click(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && TrashSlot.click(leftPos, topPos, mouseX, mouseY, menu, true)) {
            return true;
        }
        if (button == 0 && insideScrollbar(mouseX, mouseY)) {
            scrolling = menu.canScroll();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            int top = topPos + GRID_Y;
            scrollOffs = Mth.clamp(((float) mouseY - top - 7.5f) / (SCROLL_H - 15f), 0, 1);
            menu.scrollTo(scrollOffs);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!menu.canScroll()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffs = menu.subtractScroll(scrollOffs, scrollY);
        menu.scrollTo(scrollOffs);
        return true;
    }

    private boolean insideScrollbar(double mouseX, double mouseY) {
        double x = mouseX - leftPos - SCROLL_X, y = mouseY - topPos - GRID_Y;
        return x >= 0 && x < SCROLL_W && y >= 0 && y < SCROLL_H;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + imageWidth || mouseY >= guiTop + imageHeight;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (ignoreTextInput || !tab.hasSearchBar()) {
            return false;
        }
        String before = searchBox.getValue();
        if (searchBox.charTyped(codePoint, modifiers)) {
            if (!Objects.equals(before, searchBox.getValue())) {
                refreshSearchResults();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ignoreTextInput = false;
        if (!tab.hasSearchBar()) {
            // клавиша чата — к поиску, как в ванильном креативе
            if (minecraft.options.keyChat.matches(keyCode, scanCode)) {
                ignoreTextInput = true;
                showTab(CreativeModeTabs.searchTab(), true);
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        boolean numberOnItem = (hoveredSlot == null || hoveredSlot.container != menu.grid || hoveredSlot.hasItem())
                && com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        if (numberOnItem && checkHotbarKeyPressed(keyCode, scanCode)) {
            ignoreTextInput = true;
            return true;
        }
        String before = searchBox.getValue();
        if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            if (!Objects.equals(before, searchBox.getValue())) {
                refreshSearchResults();
            }
            return true;
        }
        return searchBox.isFocused() && searchBox.isVisible() && keyCode != 256 || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        ignoreTextInput = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** Как CreativeModeInventoryScreen.slotClicked для вкладок с предметами (вкладку «Инвентарь» заменяет свой инвентарь). */
    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        boolean quick = type == ClickType.QUICK_MOVE;
        type = slotId == -999 && type == ClickType.PICKUP ? ClickType.THROW : type;
        var gameMode = minecraft.gameMode;
        var player = minecraft.player;
        if (slot == null && type != ClickType.QUICK_CRAFT) {
            // клик мимо окна — выбросить предмет с курсора
            if (!menu.getCarried().isEmpty()) {
                if (mouseButton == 0) {
                    player.drop(menu.getCarried(), true);
                    gameMode.handleCreativeModeItemDrop(menu.getCarried());
                    menu.setCarried(ItemStack.EMPTY);
                }
                if (mouseButton == 1) {
                    ItemStack one = menu.getCarried().split(1);
                    player.drop(one, true);
                    gameMode.handleCreativeModeItemDrop(one);
                }
            }
            return;
        }
        if (slot != null && !slot.mayPickup(player)) {
            return;
        }
        if (slot != null && type != ClickType.QUICK_CRAFT && slot.container == menu.grid) {
            ItemStack carried = menu.getCarried();
            ItemStack item = slot.getItem();
            switch (type) {
                case SWAP -> {
                    if (!item.isEmpty()) {
                        player.getInventory().setItem(mouseButton, item.copyWithCount(item.getMaxStackSize()));
                        player.inventoryMenu.broadcastChanges();
                    }
                    return;
                }
                case CLONE -> {
                    if (carried.isEmpty() && slot.hasItem()) {
                        menu.setCarried(item.copyWithCount(item.getMaxStackSize()));
                    }
                    return;
                }
                case THROW -> {
                    if (!item.isEmpty()) {
                        ItemStack thrown = item.copyWithCount(mouseButton == 0 ? 1 : item.getMaxStackSize());
                        player.drop(thrown, true);
                        gameMode.handleCreativeModeItemDrop(thrown);
                    }
                    return;
                }
                default -> {
                }
            }
            if (!carried.isEmpty() && !item.isEmpty() && ItemStack.isSameItemSameComponents(carried, item)) {
                if (mouseButton == 0) {
                    if (quick) {
                        carried.setCount(carried.getMaxStackSize());
                    } else if (carried.getCount() < carried.getMaxStackSize()) {
                        carried.grow(1);
                    }
                } else {
                    carried.shrink(1);
                }
            } else if (!item.isEmpty() && carried.isEmpty()) {
                menu.setCarried(item.copyWithCount(quick ? item.getMaxStackSize() : item.getCount()));
            } else if (mouseButton == 0) {
                menu.setCarried(ItemStack.EMPTY);
            } else if (!carried.isEmpty()) {
                carried.shrink(1);
            }
            return;
        }
        // хотбар: меняем на клиенте и отправляем серверу (слоты 36..44 меню инвентаря игрока)
        ItemStack before = slot == null ? ItemStack.EMPTY : menu.getSlot(slot.index).getItem();
        menu.clicked(slot == null ? slotId : slot.index, mouseButton, type, player);
        int hotbarStart = menu.slots.size() - 9;
        if (AbstractContainerMenu.getQuickcraftHeader(mouseButton) == 2) {
            for (int k = 0; k < 9; k++) {
                gameMode.handleCreativeModeItemAdd(menu.getSlot(hotbarStart + k).getItem(), 36 + k);
            }
        } else if (slot != null) {
            gameMode.handleCreativeModeItemAdd(menu.getSlot(slot.index).getItem(), slot.index - hotbarStart + 36);
            if (type == ClickType.SWAP) {
                gameMode.handleCreativeModeItemAdd(before, mouseButton + 36);
            } else if (type == ClickType.THROW && !before.isEmpty()) {
                ItemStack thrown = before.copyWithCount(mouseButton == 0 ? 1 : before.getMaxStackSize());
                player.drop(thrown, true);
                gameMode.handleCreativeModeItemDrop(thrown);
            }
            player.inventoryMenu.broadcastChanges();
        }
    }

    /** Меню окна: сетка предметов вкладки (только на клиенте) и хотбар; предмет на курсоре — у меню инвентаря игрока. */
    public static class PickerMenu extends AbstractContainerMenu {
        public final List<ItemStack> items = Lists.newArrayList();
        final SimpleContainer grid = new SimpleContainer(COLS * ROWS);
        private final AbstractContainerMenu inventoryMenu;

        PickerMenu(Player player) {
            super(null, 0);
            inventoryMenu = player.inventoryMenu;
            Inventory inventory = player.getInventory();
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    addSlot(new Slot(grid, r * COLS + c, GRID_X + c * 18, GRID_Y + r * 18) {
                        @Override
                        public boolean mayPickup(Player p) {
                            // как CustomCreativeSlot: заблокированные (пустые слоты вкладки) не берутся
                            ItemStack stack = getItem();
                            return super.mayPickup(p) && (stack.isEmpty()
                                    || stack.get(net.minecraft.core.component.DataComponents.CREATIVE_SLOT_LOCK) == null);
                        }
                    });
                }
            }
            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(inventory, k, TrailInventoryMenu.MAIN_X + 1 + k * 18, TrailInventoryMenu.HOTBAR_Y + 1));
            }
        }

        private int hiddenRows() {
            return Math.max(0, Mth.positiveCeilDiv(items.size(), COLS) - ROWS);
        }

        int rowForScroll(float scroll) {
            return Math.max((int) (scroll * hiddenRows() + 0.5), 0);
        }

        float scrollForRow(int row) {
            return hiddenRows() == 0 ? 0 : Mth.clamp((float) row / hiddenRows(), 0, 1);
        }

        float subtractScroll(float scroll, double input) {
            return hiddenRows() == 0 ? 0 : Mth.clamp(scroll - (float) (input / hiddenRows()), 0, 1);
        }

        /** Показывает в сетке ряды начиная с того, что выбран прокруткой. */
        public void scrollTo(float scroll) {
            int first = rowForScroll(scroll);
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    int i = c + (r + first) * COLS;
                    grid.setItem(c + r * COLS, i >= 0 && i < items.size() ? items.get(i) : ItemStack.EMPTY);
                }
            }
        }

        public boolean canScroll() {
            return items.size() > COLS * ROWS;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        /** Shift-клик по хотбару очищает слот, как в ванильном креативе. */
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            if (index >= slots.size() - 9 && index < slots.size()) {
                Slot slot = slots.get(index);
                if (slot.hasItem()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
            return slot.container != grid;
        }

        @Override
        public boolean canDragTo(Slot slot) {
            return slot.container != grid;
        }

        @Override
        public ItemStack getCarried() {
            return inventoryMenu.getCarried();
        }

        @Override
        public void setCarried(ItemStack stack) {
            inventoryMenu.setCarried(stack);
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.List;
import java.util.function.Consumer;

/**
 * Вкладки креатива над панелью инвентаря TrailCraft и своего креативного окна (TrailCreativeScreen) — оба одного
 * размера, поэтому вкладки стоят на одном месте: ряд вдоль верхнего края панели, справа — «Инвентарь». Не влезли все —
 * листаются кнопками «<» и «>» над рядом, как в ванильном креативе. Выбранная вкладка сливается с панелью.
 * Вкладка сохранённых хотбаров не показывается.
 */
public final class CreativeTabs {
    /** Вкладка 26×32 (ванильные спрайты: _1 — у левого края панели, _2 — в середине, _7 — у правого), шаг 27; над панелью торчит на 28, кнопки листания — ещё выше. */
    public static final int TAB_W = 26, TAB_H = 32, STEP = 27, RISE = 28, PAGE_BUTTONS = 50, PAGE_GAP = 24;
    private static final ResourceLocation[] UNSELECTED = sprites("unselected"), SELECTED = sprites("selected");

    private final List<CreativeModeTab> tabs;
    private final CreativeModeTab inventory, selected;
    private final Consumer<CreativeModeTab> onSelect;
    private final int perPage;
    private int page;

    /** selected — выбранная вкладка (рисуется слитой с панелью), onSelect — что делать по клику на другую. */
    public CreativeTabs(int panelWidth, CreativeModeTab selected, Consumer<CreativeModeTab> onSelect) {
        tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY || tab.getType() == CreativeModeTab.Type.SEARCH)
                .filter(CreativeModeTab::shouldDisplay).toList();
        inventory = inventoryTab();
        this.selected = selected;
        this.onSelect = onSelect;
        perPage = Math.max(1, (panelWidth - STEP) / STEP);
        int index = tabs.indexOf(selected);
        page = index < 0 ? 0 : index / perPage;
    }

    /** Ванильная вкладка «Инвентарь» (в TrailCraft — инвентарь TrailCraft). */
    public static CreativeModeTab inventoryTab() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INVENTORY);
    }

    /** Вкладки видны только в креативе. */
    public static boolean shown() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameMode != null && mc.gameMode.hasInfiniteItems();
    }

    private int pages() {
        return Math.max(1, (tabs.size() + perPage - 1) / perPage);
    }

    /**
     * Кнопки листания над рядом (если страниц больше одной) — по сторонам от номера страницы, а не у краёв панели:
     * у левого края экрана — кнопки карты, голосового чата и т. д. (CornerButtons).
     */
    public void addButtons(int left, int top, int panelWidth, Consumer<Button> add) {
        if (pages() > 1) {
            int center = left + panelWidth / 2;
            add.accept(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                    .bounds(center - PAGE_GAP - 20, top - PAGE_BUTTONS, 20, 20).build());
            add.accept(Button.builder(Component.literal(">"), b -> page = Math.min(pages() - 1, page + 1))
                    .bounds(center + PAGE_GAP, top - PAGE_BUTTONS, 20, 20).build());
        }
    }

    /** Невыбранные вкладки — до панели (их низ прячется под ней). */
    public void renderBehind(GuiGraphics g, int left, int top, int panelWidth) {
        for (int i = 0; i < visible(); i++) {
            CreativeModeTab tab = tabs.get(page * perPage + i);
            if (tab != selected) {
                tab(g, UNSELECTED[i == 0 ? 0 : 1], tab, left + STEP * i, top);
            }
        }
        if (inventory != null && inventory != selected) {
            tab(g, UNSELECTED[6], inventory, left + panelWidth - TAB_W, top);
        }
    }

    /** Выбранная вкладка — поверх края панели; номер страницы над рядом. */
    public void renderFront(GuiGraphics g, Font font, int left, int top, int panelWidth) {
        if (selected == inventory && inventory != null) {
            tab(g, SELECTED[6], inventory, left + panelWidth - TAB_W, top);
        } else {
            int i = tabs.indexOf(selected) - page * perPage;
            if (i >= 0 && i < visible()) {
                tab(g, SELECTED[i == 0 ? 0 : 1], selected, left + STEP * i, top);
            }
        }
        if (pages() > 1) {
            Component text = Component.literal((page + 1) + " / " + pages());
            g.drawString(font, text, left + (panelWidth - font.width(text)) / 2, top - PAGE_BUTTONS + 6, 0xFFFFFFFF, true);
        }
    }

    private static void tab(GuiGraphics g, ResourceLocation sprite, CreativeModeTab tab, int x, int top) {
        int y = top - RISE;
        g.blitSprite(sprite, x, y, TAB_W, TAB_H);
        g.pose().pushPose();
        g.pose().translate(0, 0, 100);
        g.renderItem(tab.getIconItem(), x + 5, y + 9);
        g.pose().popPose();
    }

    /** Название вкладки под курсором. */
    public void renderTooltip(GuiGraphics g, Font font, int left, int top, int panelWidth, int mouseX, int mouseY) {
        CreativeModeTab tab = tabAt(left, top, panelWidth, mouseX, mouseY);
        if (tab != null) {
            g.renderTooltip(font, tab.getDisplayName(), mouseX, mouseY);
        }
    }

    /** Клик по вкладке. @return был ли клик по вкладке */
    public boolean click(int left, int top, int panelWidth, double mouseX, double mouseY) {
        CreativeModeTab tab = tabAt(left, top, panelWidth, mouseX, mouseY);
        if (tab == null) {
            return false;
        }
        if (tab != selected) {
            onSelect.accept(tab);
        }
        return true;
    }

    /** Где вкладки и кнопки листания (чтобы JEI их не закрывал). */
    public Rect2i area(int left, int top, int panelWidth) {
        int rise = pages() > 1 ? PAGE_BUTTONS : RISE;
        return new Rect2i(left, top - rise, panelWidth, rise);
    }

    private CreativeModeTab tabAt(int left, int top, int panelWidth, double mouseX, double mouseY) {
        // торчащая над панелью часть вкладки
        if (mouseY < top - RISE || mouseY >= top) {
            return null;
        }
        if (inventory != null && mouseX >= left + panelWidth - TAB_W && mouseX < left + panelWidth) {
            return inventory;
        }
        int i = (int) Math.floor((mouseX - left) / STEP);
        if (mouseX < left || i >= visible() || mouseX - left - STEP * i >= TAB_W) {
            return null;
        }
        return tabs.get(page * perPage + i);
    }

    private int visible() {
        return Math.max(0, Math.min(perPage, tabs.size() - page * perPage));
    }

    private static ResourceLocation[] sprites(String kind) {
        ResourceLocation[] sprites = new ResourceLocation[7];
        for (int i = 0; i < 7; i++) {
            sprites[i] = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_" + kind + "_" + (i + 1));
        }
        return sprites;
    }
}

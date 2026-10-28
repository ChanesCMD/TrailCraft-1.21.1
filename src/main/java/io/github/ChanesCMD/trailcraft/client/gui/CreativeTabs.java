package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.mixin.CreativeScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.List;
import java.util.function.Consumer;

/**
 * Вкладки креатива над инвентарём TrailCraft (в креативе он и есть вкладка «Инвентарь», см. ClientCreativeEvents):
 * ряд вкладок вдоль верхнего края панели, справа — выбранная «Инвентарь». Не влезли все — листаются кнопками
 * «<» и «>» над рядом, как в креативном окне. Клик по вкладке открывает креативное окно на ней.
 */
public final class CreativeTabs {
    /** Вкладка 26×32 (ванильные спрайты: _1 — у левого края панели, _2 — в середине, _7 — у правого), шаг 27; над панелью торчит на 28, кнопки листания — ещё выше. */
    public static final int TAB_W = 26, TAB_H = 32, STEP = 27, RISE = 28, PAGE_BUTTONS = 50;
    private static final ResourceLocation[] UNSELECTED = sprites("unselected"), SELECTED = sprites("selected");

    private final List<CreativeModeTab> tabs;
    private final CreativeModeTab inventory;
    private final int perPage;
    private int page;

    public CreativeTabs(int panelWidth) {
        tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(tab -> tab.getType() != CreativeModeTab.Type.INVENTORY && tab.shouldDisplay()).toList();
        inventory = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY).findFirst().orElse(null);
        perPage = Math.max(1, (panelWidth - STEP) / STEP);
    }

    /** Вкладки видны только в креативе. */
    public static boolean shown() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameMode != null && mc.gameMode.hasInfiniteItems();
    }

    private int pages() {
        return Math.max(1, (tabs.size() + perPage - 1) / perPage);
    }

    /** Кнопки листания над рядом (если страниц больше одной). */
    public void addButtons(int left, int top, int panelWidth, Consumer<Button> add) {
        if (pages() > 1) {
            add.accept(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                    .bounds(left, top - PAGE_BUTTONS, 20, 20).build());
            add.accept(Button.builder(Component.literal(">"), b -> page = Math.min(pages() - 1, page + 1))
                    .bounds(left + panelWidth - 20, top - PAGE_BUTTONS, 20, 20).build());
        }
    }

    /** Невыбранные вкладки — до панели (их низ прячется под ней). */
    public void renderBehind(GuiGraphics g, int left, int top) {
        for (int i = 0; i < visible(); i++) {
            CreativeModeTab tab = tabs.get(page * perPage + i);
            int x = left + STEP * i, y = top - RISE;
            g.blitSprite(UNSELECTED[i == 0 ? 0 : 1], x, y, TAB_W, TAB_H);
            g.renderItem(tab.getIconItem(), x + 5, y + 9);
        }
    }

    /** Выбранная «Инвентарь» — поверх края панели; номер страницы над рядом. */
    public void renderFront(GuiGraphics g, Font font, int left, int top, int panelWidth) {
        if (inventory != null) {
            int x = left + panelWidth - TAB_W, y = top - RISE;
            g.blitSprite(SELECTED[6], x, y, TAB_W, TAB_H);
            g.renderItem(inventory.getIconItem(), x + 5, y + 9);
        }
        if (pages() > 1) {
            Component text = Component.literal((page + 1) + " / " + pages());
            g.drawString(font, text, left + (panelWidth - font.width(text)) / 2, top - PAGE_BUTTONS + 6, 0xFFFFFFFF, true);
        }
    }

    /** Название вкладки под курсором. */
    public void renderTooltip(GuiGraphics g, Font font, int left, int top, int panelWidth, int mouseX, int mouseY) {
        CreativeModeTab tab = tabAt(left, top, panelWidth, mouseX, mouseY);
        if (tab != null) {
            g.renderTooltip(font, tab.getDisplayName(), mouseX, mouseY);
        }
    }

    /** Клик по вкладке: креативное окно на ней. @return был ли клик по вкладке */
    public boolean click(int left, int top, int panelWidth, double mouseX, double mouseY) {
        CreativeModeTab tab = tabAt(left, top, panelWidth, mouseX, mouseY);
        if (tab == null) {
            return false;
        }
        if (tab != inventory) {
            Minecraft mc = Minecraft.getInstance();
            CreativeScreenAccessor.trailcraft$setSelectedTab(tab);
            mc.player.closeContainer();
            mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(),
                    mc.options.operatorItemsTab().get()));
        }
        return true;
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

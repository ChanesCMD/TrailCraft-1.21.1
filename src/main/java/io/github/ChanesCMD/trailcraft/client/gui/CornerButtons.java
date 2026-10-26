package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.client.map.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Кнопки в левом верхнем углу экрана инвентаря (ТЗ, раздел 6): 14×14 на (4,4) с шагом 16, иконки 10×10.
 * JEI — только если он установлен; это вместо шестерёнки самого JEI (её убирает миксин): клик — настройки JEI
 * (экран настроек или, если его нет, сообщение в чат с папкой конфигурации, как делала шестерёнка), Ctrl+клик — режим
 * читера, Shift+клик — «список предметов только при поиске». Карта — если в рюкзаке атлас. Приваты и гайдбук пока
 * заглушки «Скоро».
 */
public final class CornerButtons {
    public static final int X = 4, Y = 4, SIZE = 14, STEP = 16;

    /** Иконки 10×10: '#' — тёмный пиксель. */
    private static final String[] MAGNIFIER = {
            "..####....", ".#....#...", "#......#..", "#......#..", "#......#..",
            "#......#..", ".#....#...", "..#####...", "......###.", ".......###"};
    private static final String[] MAP = {
            "##.##.##..", "#.##.##.#.", "#..#..#.#.", "#..#..#.#.", "#..#..#.#.",
            "#..#..#.#.", "#..#..#.#.", "#.##.##.#.", "##.##.##..", ".........."};
    private static final String[] MIC = {
            "...####...", "...####...", "...####...", "...####...", "...####...",
            ".#.####.#.", ".#......#.", "..######..", "....##....", "..######.."};
    private static final String[] FLAG = {
            "#.........", "########..", "#######...", "######....", "#######...",
            "########..", "#.........", "#.........", "#.........", "#........."};
    private static final String[] GUIDE = {
            "..######..", ".##....##.", ".##....##.", "......##..", ".....##...",
            "....##....", "....##....", "..........", "....##....", "....##...."};

    public record Entry(String id, String[] icon, Runnable action) {}

    /** Кнопки по порядку; voice — что делать по кнопке голосового чата. */
    public static List<Entry> create(Screen parent, Runnable voice) {
        List<Entry> list = new ArrayList<>();
        if (ModList.get().isLoaded("jei")) {
            list.add(new Entry("jei", MAGNIFIER, () -> jeiClick(parent)));
        }
        list.add(new Entry("map", MAP, CornerButtons::openMap));
        list.add(new Entry("voice", MIC, voice));
        list.add(new Entry("claims", FLAG, CornerButtons::soon));
        list.add(new Entry("guide", GUIDE, CornerButtons::soon));
        return list;
    }

    /** Что делала шестерёнка JEI: Ctrl — режим читера, Shift — список только при поиске, иначе настройки. */
    private static void jeiClick(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (Screen.hasControlDown()) {
            if (JeiBridge.toggleCheatMode()) {
                message("message.trailcraft.jei.cheat." + (JeiBridge.cheatMode() ? "on" : "off"));
            } else {
                soon();
            }
        } else if (Screen.hasShiftDown()) {
            if (JeiBridge.toggleHideWhenNotSearching()) {
                message("message.trailcraft.jei.hide." + (Boolean.TRUE.equals(JeiBridge.hideWhenNotSearching()) ? "on" : "off"));
            } else {
                soon();
            }
        } else {
            ModList.get().getModContainerById("jei")
                    .flatMap(container -> container.getCustomExtension(IConfigScreenFactory.class).map(f -> f.createScreen(container, parent)))
                    .ifPresentOrElse(mc::setScreen, CornerButtons::jeiConfigMessage);
        }
    }

    /** Экрана настроек у JEI нет: как и шестерёнка JEI, пишем в чат ссылку на мод с экраном и папку конфигурации. */
    private static void jeiConfigMessage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Component link = Component.translatable("jei.message.mezzConfigGui").setStyle(Style.EMPTY
                .withColor(ChatFormatting.DARK_BLUE).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/mezzconfiggui")));
        Component folder = Component.translatable("jei.message.config.folder").setStyle(Style.EMPTY
                .withColor(ChatFormatting.WHITE).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                        FMLPaths.CONFIGDIR.get().resolve("jei").toAbsolutePath().toString())));
        mc.player.displayClientMessage(link.copy().append("\n").append(folder), false);
    }

    private static void message(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private static void openMap() {
        MapScreen.open();
    }

    private static void soon() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.trailcraft.soon"), true);
        }
    }

    public static int x(int index) {
        return X + index * STEP;
    }

    /** Кнопка под курсором или -1. */
    public static int hovered(List<Entry> entries, double mouseX, double mouseY) {
        for (int i = 0; i < entries.size(); i++) {
            if (mouseX >= x(i) && mouseX < x(i) + SIZE && mouseY >= Y && mouseY < Y + SIZE) {
                return i;
            }
        }
        return -1;
    }

    public static void render(GuiGraphics g, Font font, List<Entry> entries, int mouseX, int mouseY, int active) {
        int hover = hovered(entries, mouseX, mouseY);
        for (int i = 0; i < entries.size(); i++) {
            int x = x(i);
            boolean lit = i == hover || i == active;
            // Выпуклая кнопка: светлая сверху-слева, тёмная снизу-справа
            UiDraw.frame(g, x, Y, SIZE, SIZE, 0xFF000000, 0xFF000000, 0xFF000000);
            g.fill(x + 1, Y + 1, x + SIZE - 1, Y + SIZE - 1, lit ? 0xFFE0E0E0 : 0xFFFFFFFF);
            g.fill(x + 2, Y + 2, x + SIZE - 1, Y + SIZE - 1, 0xFF555555);
            g.fill(x + 2, Y + 2, x + SIZE - 2, Y + SIZE - 2, lit ? 0xFFB0B0D8 : UiDraw.PANEL);
            icon(g, x + 2, Y + 2, entries.get(i).icon());
            if (entries.get(i).id().equals("jei") && JeiBridge.cheatMode()) {
                g.fill(x + SIZE - 5, Y + SIZE - 5, x + SIZE - 2, Y + SIZE - 2, 0xFFD83A3A); // режим читера включён
            }
        }
        if (hover >= 0) {
            g.renderComponentTooltip(font, tooltip(entries.get(hover)), mouseX, mouseY);
        }
    }

    /** Подсказка кнопки; у JEI ещё и что делают Ctrl и Shift, и включённые режимы. */
    private static List<Component> tooltip(Entry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.trailcraft.button." + entry.id()));
        if (entry.id().equals("jei")) {
            lines.add(Component.translatable("gui.trailcraft.button.jei.cheat").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.trailcraft.button.jei.hide").withStyle(ChatFormatting.GRAY));
            if (JeiBridge.cheatMode()) {
                lines.add(Component.translatable("gui.trailcraft.button.jei.cheat.on").withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    private static void icon(GuiGraphics g, int x, int y, String[] pattern) {
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length(); col++) {
                if (pattern[row].charAt(col) == '#') {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, 0xFF3A3A3A);
                }
            }
        }
    }

    private CornerButtons() {}
}

package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.client.map.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Кнопки в левом верхнем углу экрана инвентаря (ТЗ, раздел 6): 14×14 на (4,4) с шагом 16, иконки 10×10.
 * JEI — только если он установлен. Карта — если в рюкзаке атлас. Приваты и гайдбук пока заглушки «Скоро».
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
            list.add(new Entry("jei", MAGNIFIER, () -> openJeiConfig(parent)));
        }
        list.add(new Entry("map", MAP, CornerButtons::openMap));
        list.add(new Entry("voice", MIC, voice));
        list.add(new Entry("claims", FLAG, CornerButtons::soon));
        list.add(new Entry("guide", GUIDE, CornerButtons::soon));
        return list;
    }

    /** Настройки JEI — его экран конфигурации из списка модов. */
    private static void openJeiConfig(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        ModList.get().getModContainerById("jei")
                .flatMap(container -> container.getCustomExtension(IConfigScreenFactory.class).map(f -> f.createScreen(container, parent)))
                .ifPresentOrElse(mc::setScreen, CornerButtons::soon);
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
        }
        if (hover >= 0) {
            g.renderTooltip(font, Component.translatable("gui.trailcraft.button." + entries.get(hover).id()), mouseX, mouseY);
        }
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

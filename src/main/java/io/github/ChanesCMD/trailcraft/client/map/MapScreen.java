package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Полноэкранная карта (макет docs/ui/08_map.png): сверху тёмная полоса с названием, кнопками настроек и закрытия,
 * внизу координаты точки под курсором (или игрока). Нужен атлас в рюкзаке (проверяет кнопка, которая открывает карту).
 * Колесо — масштаб (1, 2, 4 или 8 px на блок), ЛКМ с перетаскиванием — сдвиг, ПКМ — снова к игроку.
 * Видно то, что загружено у клиента. Текстура перерисовывается понемногу каждый кадр (ROWS_BUDGET блоков).
 */
public class MapScreen extends Screen {
    private static final int BAR_H = 18, BUTTON = 14, ROWS_BUDGET = 16384;
    private static final int[] ZOOMS = {1, 2, 4, 8};
    private static final String[] GEAR = {
            "...##.##...", "..#######..", ".##.....##.", "..#..#..#..", "##..###..##",
            "##..###..##", "..#..#..#..", ".##.....##.", "..#######..", "...##.##...", "..........."};
    private final MapTexture texture = new MapTexture("map");
    private int zoomIndex = 1;
    /** Центр карты в блоках; пока игрок не сдвинул карту, она следует за ним. */
    private double centerX, centerZ;
    private boolean following = true;
    /** Область, нарисованная в текстуре (левый верхний блок), и сколько строк уже перерисовано. */
    private int left, top, row;
    private boolean dragging;

    public MapScreen() {
        super(Component.translatable("gui.trailcraft.map"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft.player != null && following) {
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
        }
        restart();
    }

    private int zoom() {
        return ZOOMS[zoomIndex];
    }

    private int mapHeight() {
        return height - BAR_H;
    }

    /** Заново привязывает текстуру к центру карты и начинает перерисовку с первой строки (старое видно до неё). */
    private void restart() {
        texture.resize(Mth.ceil(width / (float) zoom()) + 1, Mth.ceil(mapHeight() / (float) zoom()) + 1);
        left = Mth.floor(centerX) - texture.width() / 2;
        top = Mth.floor(centerZ) - texture.height() / 2;
        row = 0;
    }

    @Override
    public void tick() {
        Player player = minecraft.player;
        if (player != null && following && !dragging) {
            centerX = player.getX();
            centerZ = player.getZ();
            // Игрок ушёл далеко от середины нарисованного — привязать заново
            if (Math.abs(Mth.floor(centerX) - (left + texture.width() / 2)) > 8 || Math.abs(Mth.floor(centerZ) - (top + texture.height() / 2)) > 8) {
                restart();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        // Понемногу перерисовываем: за кадр — ROWS_BUDGET блоков, потом по кругу
        int rows = Math.max(1, ROWS_BUDGET / Math.max(1, texture.width()));
        texture.paint(player.level(), left, top, row, row + rows);
        row += rows;
        if (row >= texture.height()) {
            row = 0;
        }
        texture.upload();

        int zoom = zoom();
        g.fill(0, BAR_H, width, height, 0xFF000000);
        // Где на экране блок (left, top): центр карты — в середине области под полосой
        double originX = width / 2.0 - (centerX - left) * zoom, originY = BAR_H + mapHeight() / 2.0 - (centerZ - top) * zoom;
        g.enableScissor(0, BAR_H, width, height);
        g.blit(texture.location(), (int) Math.round(originX), (int) Math.round(originY), texture.width() * zoom, texture.height() * zoom,
                0, 0, texture.width(), texture.height(), texture.width(), texture.height());
        int px = (int) Math.round(originX + (player.getX() - left) * zoom), py = (int) Math.round(originY + (player.getZ() - top) * zoom);
        Minimap.playerMarker(g, px, py, player.getDirection());
        g.disableScissor();

        // Полоса сверху: название, настройки, закрыть
        g.fill(0, 0, width, BAR_H, 0xFF1E1E1E);
        g.drawString(font, title, 8, (BAR_H - 8) / 2, 0xFFFFFFFF, false);
        int closeX = width - 4 - BUTTON, gearX = closeX - 4 - BUTTON, by = (BAR_H - BUTTON) / 2;
        button(g, gearX, by, mouseX, mouseY);
        icon(g, gearX + 2, by + 2, GEAR);
        button(g, closeX, by, mouseX, mouseY);
        for (int i = 0; i < 8; i++) {
            g.fill(closeX + 3 + i, by + 3 + i, closeX + 4 + i, by + 4 + i, 0xFF3A3A3A);
            g.fill(closeX + 10 - i, by + 3 + i, closeX + 11 - i, by + 4 + i, 0xFF3A3A3A);
        }

        // Координаты внизу слева: точка под курсором, а если курсор не на карте — игрок
        int bx, bz;
        if (mouseY >= BAR_H) {
            bx = Mth.floor(left + (mouseX - originX) / zoom);
            bz = Mth.floor(top + (mouseY - originY) / zoom);
        } else {
            bx = player.getBlockX();
            bz = player.getBlockZ();
        }
        g.drawString(font, Component.translatable("gui.trailcraft.map.coords", bx, bz), 8, height - 14, 0xFFFFFFFF, true);
        Component hint = Component.translatable("gui.trailcraft.map.hint");
        g.drawString(font, hint, width - 8 - font.width(hint), height - 14, 0xFFC0C0C0, true);

        if (isOver(mouseX, mouseY, gearX, by)) {
            g.renderTooltip(font, Component.translatable("gui.trailcraft.map.settings"), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, closeX, by)) {
            g.renderTooltip(font, Component.translatable("gui.trailcraft.map.close"), mouseX, mouseY);
        }
    }

    /** Кнопка 14×14 в стиле кнопок в углу инвентаря. */
    private void button(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        boolean lit = isOver(mouseX, mouseY, x, y);
        UiDraw.frame(g, x, y, BUTTON, BUTTON, 0xFF000000, 0xFF000000, 0xFF000000);
        g.fill(x + 1, y + 1, x + BUTTON - 1, y + BUTTON - 1, lit ? 0xFFE0E0E0 : 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + BUTTON - 1, y + BUTTON - 1, 0xFF555555);
        g.fill(x + 2, y + 2, x + BUTTON - 2, y + BUTTON - 2, lit ? 0xFFB0B0D8 : UiDraw.PANEL);
    }

    private static void icon(GuiGraphics g, int x, int y, String[] pattern) {
        for (int r = 0; r < pattern.length; r++) {
            for (int c = 0; c < pattern[r].length(); c++) {
                if (pattern[r].charAt(c) == '#') {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, 0xFF3A3A3A);
                }
            }
        }
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BUTTON && mouseY >= y && mouseY < y + BUTTON;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int closeX = width - 4 - BUTTON, by = (BAR_H - BUTTON) / 2;
        if (button == 0 && isOver(mouseX, mouseY, closeX, by)) {
            onClose();
            return true;
        }
        if (mouseY < BAR_H) {
            return true; // настройки карты пока не сделаны (подсказка «Скоро»)
        }
        if (button == 0) {
            dragging = true;
            return true;
        }
        if (button == 1 && minecraft.player != null) {
            following = true;
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
            restart();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            following = false;
            centerX -= dragX / zoom();
            centerZ -= dragY / zoom();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            restart();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Mth.clamp(zoomIndex + (int) Math.signum(scrollY), 0, ZOOMS.length - 1);
        if (next != zoomIndex) {
            zoomIndex = next;
            restart();
        }
        return true;
    }

    @Override
    public void removed() {
        texture.close();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

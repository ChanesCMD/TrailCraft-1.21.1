package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.Coords;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.Util;
import io.github.ChanesCMD.trailcraft.client.ClientGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import io.github.ChanesCMD.trailcraft.client.map.Waypoints.Waypoint;
import io.github.ChanesCMD.trailcraft.map.MapTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Полноэкранная карта (макет docs/ui/08_map.png): сверху тёмная полоса с названием, кнопками настроек и закрытия,
 * внизу координаты точки под курсором (или игрока). Нужен атлас в рюкзаке (проверяет кнопка, которая открывает карту).
 * Колесо — плавный масштаб (0,5–8 px на блок), перетаскивание — сдвиг, кнопка в полосе сверху — снова к игроку,
 * ПКМ — контекстное меню: путевые точки (добавить, изменить, удалить) и телепорт на точку для админов.
 * Карта плавно следует за игроком. Видно всё, что атлас запомнил (MapCache), а не только загруженное.
 */
public class MapScreen extends Screen {
    private static final int BAR_H = 18, BUTTON = 14;
    private static final float[] ZOOMS = {0.5f, 1, 2, 4, 8};
    private static final String[] GEAR = {
            "...##.##...", "..#######..", ".##.....##.", "..#..#..#..", "##..###..##",
            "##..###..##", "..#..#..#..", ".##.....##.", "..#######..", "...##.##...", "..........."};
    private int zoomIndex = 2;
    /** Масштаб сейчас: плавно догоняет ZOOMS[zoomIndex]. */
    private float zoom = ZOOMS[2];
    /** Центр карты в блоках; пока игрок не сдвинул карту, она следует за ним. */
    private double centerX, centerZ;
    private boolean following = true;
    private boolean dragging;
    private long lastFrame;
    /** Контекстное меню (ПКМ): пункт без действия — заголовок. Пока оно открыто, список не пуст. */
    private final List<MenuItem> menu = new ArrayList<>();
    private int menuX, menuY, menuW;

    private record MenuItem(Component label, @Nullable Runnable action) {}

    private static final int ITEM_H = 12;
    private static final String[] TARGET = {
            "....###....", "..##.#.##..", ".#...#...#.", ".#...#...#.", "#....#....#", "#####.#####",
            "#....#....#", ".#...#...#.", ".#...#...#.", "..##.#.##..", "....###...."};

    public MapScreen() {
        super(Component.translatable("gui.trailcraft.map"));
    }

    /** Открывает карту, если в рюкзаке атлас (иначе сообщение). Открытый инвентарь при этом закрывается. */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!ClientGear.hasAtlas()) {
            mc.player.displayClientMessage(Component.translatable("message.trailcraft.map.need"), true);
            return;
        }
        if (mc.screen != null) {
            mc.player.closeContainer();
        }
        mc.setScreen(new MapScreen());
    }

    private int mapHeight() {
        return height - BAR_H;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        // Плавный масштаб: за ~0.15 с догоняет выбранный
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float target = ZOOMS[zoomIndex];
        zoom = Math.abs(target - zoom) < 0.01f ? target : zoom + (target - zoom) * Math.min(1, dt * 12);

        Vec3 pos = player.getPosition(partialTick);
        if (following) {
            centerX = pos.x;
            centerZ = pos.z;
        }
        g.fill(0, BAR_H, width, height, 0xFF000000);
        MapTiles.draw(g, player.level().dimension().location(), 0, BAR_H, width, mapHeight(), centerX, centerZ, zoom);
        double midX = width / 2.0, midY = BAR_H + mapHeight() / 2.0;
        ResourceLocation dimension = player.level().dimension().location();
        Waypoint hovered = menu.isEmpty() ? waypointAt(dimension, mouseX, mouseY) : null;
        g.enableScissor(0, BAR_H, width, height);
        for (Waypoint waypoint : Waypoints.inDimension(dimension)) {
            int sx = screenX(waypoint.x() + 0.5), sy = screenY(waypoint.z() + 0.5);
            if (sx > -100 && sx < width + 100 && sy > BAR_H - 10 && sy < height + 10) {
                drawWaypoint(g, sx, sy, waypoint, waypoint == hovered);
            }
        }
        g.disableScissor();
        int px = (int) Math.round(midX + (pos.x - centerX) * zoom), py = (int) Math.round(midY + (pos.z - centerZ) * zoom);
        if (py >= BAR_H) {
            Minimap.playerMarker(g, px, py, player.getViewYRot(partialTick));
        }

        // Полоса сверху: название, настройки, закрыть
        g.fill(0, 0, width, BAR_H, 0xFF1E1E1E);
        g.drawString(font, title, 8, (BAR_H - 8) / 2, 0xFFFFFFFF, false);
        int closeX = width - 4 - BUTTON, gearX = closeX - 4 - BUTTON, followX = gearX - 4 - BUTTON, by = (BAR_H - BUTTON) / 2;
        button(g, followX, by, mouseX, mouseY);
        icon(g, followX + 2, by + 2, TARGET);
        button(g, gearX, by, mouseX, mouseY);
        icon(g, gearX + 2, by + 2, GEAR);
        button(g, closeX, by, mouseX, mouseY);
        for (int i = 0; i < 8; i++) {
            g.fill(closeX + 3 + i, by + 3 + i, closeX + 4 + i, by + 4 + i, 0xFF3A3A3A);
            g.fill(closeX + 10 - i, by + 3 + i, closeX + 11 - i, by + 4 + i, 0xFF3A3A3A);
        }

        // Внизу слева: координаты, биом и блок под курсором, а если курсор не на карте — под игроком
        int bx, bz;
        if (mouseY >= BAR_H) {
            bx = Mth.floor(centerX + (mouseX - midX) / zoom);
            bz = Mth.floor(centerZ + (mouseY - midY) / zoom);
        } else {
            bx = player.getBlockX();
            bz = player.getBlockZ();
        }
        g.drawString(font, Component.literal(Coords.horizontal(bx, bz)), 8, height - 14, 0xFFFFFFFF, true);
        BlockPos surface = MapSampler.surfaceAt(player.level(), bx, bz);
        if (surface != null) {
            Component biome = player.level().getBiome(surface).unwrapKey()
                    .map(key -> (Component) Component.translatable(Util.makeDescriptionId("biome", key.location())))
                    .orElse(Component.empty());
            Component block = player.level().getBlockState(surface).getBlock().getName();
            g.drawString(font, Component.translatable("gui.trailcraft.map.place", biome, block), 8, height - 25, 0xFFFFFFFF, true);
        }
        Component hint = Component.translatable("gui.trailcraft.map.hint");
        g.drawString(font, hint, width - 8 - font.width(hint), height - 14, 0xFFC0C0C0, true);

        if (!menu.isEmpty()) {
            renderMenu(g, mouseX, mouseY);
        } else if (hovered != null) {
            g.renderComponentTooltip(font, List.of(Component.literal(hovered.name()),
                    Component.literal("X: " + hovered.x() + "  Y: " + hovered.y() + "  Z: " + hovered.z())), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, followX, by)) {
            g.renderTooltip(font, Component.translatable("gui.trailcraft.map.follow"), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, gearX, by)) {
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

    private int screenX(double worldX) {
        return (int) Math.round(width / 2.0 + (worldX - centerX) * zoom);
    }

    private int screenY(double worldZ) {
        return (int) Math.round(BAR_H + mapHeight() / 2.0 + (worldZ - centerZ) * zoom);
    }

    /** Ближайшая к курсору путевая точка в пределах 6 px или null. */
    @Nullable
    private Waypoint waypointAt(ResourceLocation dimension, double mouseX, double mouseY) {
        if (mouseY < BAR_H) {
            return null;
        }
        Waypoint best = null;
        double bestDistance = 6 * 6 + 1;
        for (Waypoint waypoint : Waypoints.inDimension(dimension)) {
            double dx = mouseX - screenX(waypoint.x() + 0.5), dy = mouseY - screenY(waypoint.z() + 0.5);
            if (dx * dx + dy * dy < bestDistance) {
                best = waypoint;
                bestDistance = dx * dx + dy * dy;
            }
        }
        return best;
    }

    /** Ромб 7×7 цвета точки в чёрной обводке и название справа. */
    private void drawWaypoint(GuiGraphics g, int sx, int sy, Waypoint waypoint, boolean hovered) {
        for (int i = 0; i < 7; i++) {
            int half = 3 - Math.abs(i - 3);
            g.fill(sx - half - 1, sy - 3 + i, sx + half + 2, sy - 2 + i, 0xFF000000);
        }
        g.fill(sx, sy - 4, sx + 1, sy - 3, 0xFF000000);
        g.fill(sx, sy + 4, sx + 1, sy + 5, 0xFF000000);
        for (int i = 0; i < 7; i++) {
            int half = 3 - Math.abs(i - 3);
            g.fill(sx - half, sy - 3 + i, sx + half + 1, sy - 2 + i, 0xFF000000 | waypoint.color());
        }
        g.drawString(font, waypoint.name(), sx + 7, sy - 4, hovered ? 0xFFFFFF55 : 0xFFFFFFFF, true);
    }

    /** Открывает меню в точке экрана: у путевой точки — изменить, удалить, телепорт; на пустом месте — добавить метку, телепорт. */
    private void openMenu(double mouseX, double mouseY) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ResourceLocation dimension = player.level().dimension().location();
        Waypoint waypoint = waypointAt(dimension, mouseX, mouseY);
        boolean admin = player.hasPermissions(2);
        menu.clear();
        if (waypoint != null) {
            menu.add(new MenuItem(Component.literal(waypoint.name()), null));
            menu.add(new MenuItem(Component.translatable("gui.trailcraft.map.menu.edit"),
                    () -> minecraft.setScreen(new WaypointScreen(this, waypoint, false))));
            menu.add(new MenuItem(Component.translatable("gui.trailcraft.map.menu.delete"), () -> Waypoints.remove(waypoint)));
            if (admin) {
                menu.add(new MenuItem(Component.translatable("gui.trailcraft.map.menu.teleport"), () -> teleport(waypoint.x(), waypoint.z())));
            }
        } else {
            int bx = Mth.floor(centerX + (mouseX - width / 2.0) / zoom), bz = Mth.floor(centerZ + (mouseY - (BAR_H + mapHeight() / 2.0)) / zoom);
            BlockPos surface = MapSampler.surfaceAt(player.level(), bx, bz);
            Waypoint fresh = new Waypoint(Component.translatable("gui.trailcraft.waypoint.default", Waypoints.count() + 1).getString(),
                    dimension, bx, surface != null ? surface.getY() + 1 : player.getBlockY(), bz,
                    Waypoints.COLORS[Waypoints.count() % Waypoints.COLORS.length]);
            menu.add(new MenuItem(Component.literal(Coords.horizontal(bx, bz)), null));
            menu.add(new MenuItem(Component.translatable("gui.trailcraft.map.menu.add"),
                    () -> minecraft.setScreen(new WaypointScreen(this, fresh, true))));
            if (admin) {
                menu.add(new MenuItem(Component.translatable("gui.trailcraft.map.menu.teleport"), () -> teleport(bx, bz)));
            }
        }
        menuW = 12;
        for (MenuItem item : menu) {
            menuW = Math.max(menuW, font.width(item.label()) + 12);
        }
        menuX = Mth.clamp((int) mouseX, 2, Math.max(2, width - menuW - 2));
        menuY = Mth.clamp((int) mouseY, BAR_H, Math.max(BAR_H, height - menu.size() * ITEM_H - 6));
    }

    /** Сервер сам проверит права и подберёт высоту; карта закрывается. */
    private void teleport(int x, int z) {
        PacketDistributor.sendToServer(new MapTeleport.Payload(x, z));
        onClose();
    }

    private int menuItemAt(double mouseX, double mouseY) {
        if (mouseX < menuX || mouseX >= menuX + menuW || mouseY < menuY + 2) {
            return -1;
        }
        int index = (int) (mouseY - menuY - 2) / ITEM_H;
        return index < menu.size() ? index : -1;
    }

    private boolean insideMenu(double mouseX, double mouseY) {
        return mouseX >= menuX && mouseX < menuX + menuW && mouseY >= menuY && mouseY < menuY + menu.size() * ITEM_H + 4;
    }

    private void renderMenu(GuiGraphics g, int mouseX, int mouseY) {
        UiDraw.frame(g, menuX, menuY, menuW, menu.size() * ITEM_H + 4, 0xFF000000, 0xFF555555, 0xFF1E1E1E);
        int hovered = menuItemAt(mouseX, mouseY);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.get(i);
            int y = menuY + 2 + i * ITEM_H;
            if (i == hovered && item.action() != null) {
                g.fill(menuX + 1, y, menuX + menuW - 1, y + ITEM_H, 0xFF3A3A6A);
            }
            g.drawString(font, item.label(), menuX + 6, y + 2, item.action() == null ? 0xFFAAAAAA : 0xFFFFFFFF, false);
        }
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BUTTON && mouseY >= y && mouseY < y + BUTTON;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.isEmpty()) {
            int item = menuItemAt(mouseX, mouseY);
            Runnable action = button == 0 && item >= 0 ? menu.get(item).action() : null;
            boolean inside = insideMenu(mouseX, mouseY);
            menu.clear();
            if (action != null) {
                action.run();
                return true;
            }
            if (inside || button != 1) {
                return true; // клик по заголовку или мимо меню только закрывает его
            }
        }
        int closeX = width - 4 - BUTTON, by = (BAR_H - BUTTON) / 2;
        if (button == 0 && isOver(mouseX, mouseY, closeX, by)) {
            onClose();
            return true;
        }
        int gearX = closeX - 4 - BUTTON, followX = gearX - 4 - BUTTON;
        if (button == 0 && isOver(mouseX, mouseY, followX, by)) {
            following = true;
            return true;
        }
        if (button == 0 && isOver(mouseX, mouseY, gearX, by)) {
            minecraft.setScreen(new MapSettingsScreen(this));
            return true;
        }
        if (mouseY < BAR_H) {
            return true;
        }
        if (button == 0) {
            dragging = true;
            return true;
        }
        if (button == 1) {
            openMenu(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            following = false;
            centerX -= dragX / zoom;
            centerZ -= dragY / zoom;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        menu.clear();
        zoomIndex = Mth.clamp(zoomIndex + (int) Math.signum(scrollY), 0, ZOOMS.length - 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && !menu.isEmpty()) {
            menu.clear();
            return true;
        }
        if (MapKeys.MAP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

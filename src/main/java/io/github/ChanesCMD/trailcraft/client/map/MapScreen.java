package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.Util;
import io.github.ChanesCMD.trailcraft.client.ClientGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Полноэкранная карта (макет docs/ui/08_map.png): сверху тёмная полоса с названием, кнопками настроек и закрытия,
 * внизу координаты точки под курсором (или игрока). Нужен атлас в рюкзаке (проверяет кнопка, которая открывает карту).
 * Колесо — плавный масштаб (0,5–8 px на блок), перетаскивание — сдвиг, ПКМ — снова к игроку.
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
        int px = (int) Math.round(midX + (pos.x - centerX) * zoom), py = (int) Math.round(midY + (pos.z - centerZ) * zoom);
        if (py >= BAR_H) {
            Minimap.playerMarker(g, px, py, player.getViewYRot(partialTick));
        }

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

        // Внизу слева: координаты, биом и блок под курсором, а если курсор не на карте — под игроком
        int bx, bz;
        if (mouseY >= BAR_H) {
            bx = Mth.floor(centerX + (mouseX - midX) / zoom);
            bz = Mth.floor(centerZ + (mouseY - midY) / zoom);
        } else {
            bx = player.getBlockX();
            bz = player.getBlockZ();
        }
        g.drawString(font, Component.translatable("gui.trailcraft.map.coords", bx, bz), 8, height - 14, 0xFFFFFFFF, true);
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
        int gearX = closeX - 4 - BUTTON;
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
            following = true;
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
        zoomIndex = Mth.clamp(zoomIndex + (int) Math.signum(scrollY), 0, ZOOMS.length - 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

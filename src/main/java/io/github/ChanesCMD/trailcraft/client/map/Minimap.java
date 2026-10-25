package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Миникарта 66×66 (ТЗ, раздел 4): 64×64 блока вокруг игрока (см. MapTexture), перерисовывается раз в UPDATE_TICKS
 * тиков и сразу при смене мира. Игрок — белая точка с чёрной обводкой и выступом в сторону взгляда.
 */
public final class Minimap {
    public static final int SIZE = 66;
    private static final int MAP = SIZE - 2, UPDATE_TICKS = 10;
    private static final MapTexture TEXTURE = new MapTexture("minimap");
    private static long lastUpdate;
    private static Level lastLevel;

    public static void render(GuiGraphics g, int x, int y, Player player) {
        Level level = player.level();
        long time = level.getGameTime();
        if (level != lastLevel || time - lastUpdate >= UPDATE_TICKS || time < lastUpdate) {
            lastLevel = level;
            lastUpdate = time;
            TEXTURE.resize(MAP, MAP);
            TEXTURE.paint(level, player.getBlockX() - MAP / 2, player.getBlockZ() - MAP / 2, 0, MAP);
            TEXTURE.upload();
        }
        UiDraw.frame(g, x, y, SIZE, SIZE, 0xFF373737, 0xFFFFFFFF, 0xFF000000);
        g.blit(TEXTURE.location(), x + 1, y + 1, 0, 0, MAP, MAP, MAP, MAP);
        playerMarker(g, x + 1 + MAP / 2, y + 1 + MAP / 2, player.getDirection());
    }

    /** Белая точка 2×2 в чёрной обводке и выступ в сторону взгляда; (cx, cy) — пиксель блока игрока. */
    public static void playerMarker(GuiGraphics g, int cx, int cy, Direction facing) {
        int dx = facing.getStepX(), dz = facing.getStepZ();
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        g.fill(cx - 1 + dx * 2, cy - 1 + dz * 2, cx + 1 + dx * 2, cy + 1 + dz * 2, 0xFFFFFFFF);
    }

    private Minimap() {}
}

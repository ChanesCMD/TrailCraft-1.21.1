package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Миникарта 66×66 (ТЗ, раздел 4): 64×64 блока вокруг игрока, север сверху. Карта плавно едет за игроком
 * (позиция с учётом доли тика, см. MapView), игрок всегда в середине — белая точка с выступом в сторону взгляда.
 */
public final class Minimap {
    public static final int SIZE = 66;
    private static final int MAP = SIZE - 2, MARGIN = 16, BUDGET = 1024;
    private static final MapView VIEW = new MapView("minimap");

    public static void render(GuiGraphics g, int x, int y, Player player, float partialTick) {
        Vec3 pos = player.getPosition(partialTick);
        VIEW.update(player.level(), pos.x, pos.z, MAP + MARGIN, MAP + MARGIN, BUDGET);
        UiDraw.frame(g, x, y, SIZE, SIZE, 0xFF373737, 0xFFFFFFFF, 0xFF000000);
        VIEW.draw(g, x + 1, y + 1, MAP, MAP, pos.x, pos.z, 1);
        playerMarker(g, x + 1 + MAP / 2, y + 1 + MAP / 2, player.getDirection());
    }

    /** Белая точка 2×2 в чёрной обводке и выступ в сторону взгляда; (cx, cy) — точка игрока на экране. */
    public static void playerMarker(GuiGraphics g, int cx, int cy, Direction facing) {
        int dx = facing.getStepX(), dz = facing.getStepZ();
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        g.fill(cx - 1 + dx * 2, cy - 1 + dz * 2, cx + 1 + dx * 2, cy + 1 + dz * 2, 0xFFFFFFFF);
    }

    private Minimap() {}
}

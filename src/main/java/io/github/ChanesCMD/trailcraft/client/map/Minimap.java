package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Миникарта 66×66 (ТЗ, раздел 4): 64×64 блока вокруг игрока, север сверху. Карта плавно едет за игроком
 * (позиция с учётом доли тика, см. MapView), игрок всегда в середине — маленькая белая стрелка в 8 направлениях.
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
        playerMarker(g, x + 1 + MAP / 2, y + 1 + MAP / 2, player.getViewYRot(partialTick));
    }

    /** Стрелка 3×3 на север и на северо-восток; остальные 6 направлений — повороты на 90°. */
    private static final String[] ARROW_N = {".#.", "###", "#.#"};
    private static final String[] ARROW_NE = {"###", ".##", "#.#"};

    /**
     * Стрелка игрока 3×3 в чёрной обводке (всего 5×5), смотрит в одно из 8 направлений по взгляду;
     * (cx, cy) — точка игрока на экране, yaw — поворот игрока в градусах.
     */
    public static void playerMarker(GuiGraphics g, int cx, int cy, float yaw) {
        int dir = Math.floorMod(Math.round((yaw + 180) / 45f), 8); // 0 — север, дальше по часовой
        boolean[][] arrow = new boolean[3][3];
        String[] base = dir % 2 == 0 ? ARROW_N : ARROW_NE;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                arrow[r][c] = base[r].charAt(c) == '#';
            }
        }
        for (int turn = 0; turn < dir / 2; turn++) {
            boolean[][] rotated = new boolean[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    rotated[r][c] = arrow[2 - c][r];
                }
            }
            arrow = rotated;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (arrow[r][c]) {
                        int px = cx - 1 + c, py = cy - 1 + r;
                        if (pass == 0) {
                            g.fill(px - 1, py - 1, px + 2, py + 2, 0xFF000000);
                        } else {
                            g.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }
    }

    private Minimap() {}
}

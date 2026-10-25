package io.github.ChanesCMD.trailcraft.client.map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Карта вокруг точки, которая плавно движется (как в Xaero's Minimap): картинка в текстуре привязана к блокам,
 * при сдвиге центра на целые блоки она сдвигается и дорисовываются только открывшиеся края, а дробная часть
 * сдвига и масштаб применяются при отрисовке. Остальное понемногу перерисовывается каждый кадр (свежие блоки).
 */
public final class MapView {
    private final MapTexture texture;
    /** Блок мира в левом верхнем пикселе текстуры. */
    private int left, top;
    private int row;
    private Level level;

    public MapView(String name) {
        this.texture = new MapTexture(name);
    }

    /**
     * Готовит область width×height блоков с центром в (centerX, centerZ): сдвигает картинку, дорисовывает края
     * и перерисовывает ещё budget блоков по кругу.
     */
    public void update(Level level, double centerX, double centerZ, int width, int height, int budget) {
        int wantLeft = Mth.floor(centerX) - width / 2, wantTop = Mth.floor(centerZ) - height / 2;
        boolean fresh = texture.resize(width, height) || level != this.level;
        int dx = wantLeft - left, dz = wantTop - top;
        if (fresh || Math.abs(dx) >= width || Math.abs(dz) >= height) {
            this.level = level;
            left = wantLeft;
            top = wantTop;
            row = 0;
            // Маленькую карту рисуем сразу, большую — по кругу за несколько кадров (без подвисания)
            if (width * height <= budget * 4) {
                texture.paint(level, left, top, 0, 0, width, height);
            }
        } else if (dx != 0 || dz != 0) {
            texture.shift(dx, dz);
            left = wantLeft;
            top = wantTop;
            if (dx > 0) texture.paint(level, left, top, width - dx, 0, width, height);
            if (dx < 0) texture.paint(level, left, top, 0, 0, -dx, height);
            if (dz > 0) texture.paint(level, left, top, 0, height - dz, width, height);
            if (dz < 0) texture.paint(level, left, top, 0, 0, width, -dz);
        }
        int rows = Math.max(1, budget / width);
        texture.paint(level, left, top, 0, row, width, row + rows);
        row = row + rows >= height ? 0 : row + rows;
        texture.upload();
    }

    /**
     * Рисует карту в прямоугольнике экрана (x, y, w, h): точка мира (centerX, centerZ) — в его середине,
     * scale — пикселей интерфейса на блок (может быть дробным).
     */
    public void draw(GuiGraphics g, int x, int y, int w, int h, double centerX, double centerZ, float scale) {
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        g.pose().scale(scale, scale, 1);
        g.pose().translate((float) (left - centerX), (float) (top - centerZ), 0);
        g.blit(texture.location(), 0, 0, 0, 0, texture.width(), texture.height(), texture.width(), texture.height());
        g.pose().popPose();
        g.disableScissor();
    }

    public void close() {
        texture.close();
        level = null;
    }
}

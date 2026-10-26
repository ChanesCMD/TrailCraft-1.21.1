package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отрисовка карты плитками 64×64 блока (как регионы у Xaero's Minimap): плитка — своя текстура, которая
 * заполняется из MapCache, когда там меняются её чанки. Сдвиг и масштаб — только смещение при отрисовке,
 * поэтому карта двигается плавно и без фризов. Общие для миникарты и большой карты.
 */
public final class MapTiles {
    /** Плитка: 4×4 чанка. */
    public static final int CHUNKS = 4, SIZE = CHUNKS * 16;
    private static final int MAX_TILES = 512, PAINT_BUDGET = 24;
    private static final Map<Long, Tile> TILES = new HashMap<>();
    private static ResourceLocation dimension;
    private static int nextId;
    private static long frame;

    private static final class Tile {
        final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "map_tile_" + nextId++);
        final DynamicTexture texture = new DynamicTexture(SIZE, SIZE, true);
        boolean dirty = true, painted;
        long lastUsed;

        Tile() {
            Minecraft.getInstance().getTextureManager().register(location, texture);
        }
    }

    /** Чанки плитки (tx, tz) изменились — перерисовать при следующем показе. */
    public static void invalidate(ResourceLocation dim, int tx, int tz) {
        Tile tile = dim.equals(dimension) ? TILES.get(key(tx, tz)) : null;
        if (tile != null) {
            tile.dirty = true;
        }
    }

    /**
     * Рисует карту в прямоугольнике экрана (x, y, w, h): точка мира (centerX, centerZ) — в его середине,
     * scale — пикселей интерфейса на блок. Неисследованное прозрачно.
     */
    public static void draw(GuiGraphics g, ResourceLocation dim, int x, int y, int w, int h, double centerX, double centerZ, float scale) {
        draw(g, dim, x, y, w, h, centerX, centerZ, scale, 0, 1);
    }

    /**
     * То же, но карта повёрнута на rotation градусов по часовой стрелке вокруг своей середины (у миникарты —
     * по взгляду игрока) и нарисована с непрозрачностью alpha. Обрезается по прямоугольнику, как и без поворота.
     */
    public static void draw(GuiGraphics g, ResourceLocation dim, int x, int y, int w, int h, double centerX, double centerZ, float scale,
                            float rotation, float alpha) {
        if (!dim.equals(dimension)) {
            clear();
            dimension = dim;
        }
        frame++;
        // Повёрнутой карте нужен запас до диагонали, чтобы углы не оставались пустыми
        double halfW = (rotation == 0 ? w : Math.hypot(w, h)) / 2.0 / scale, halfH = (rotation == 0 ? h : Math.hypot(w, h)) / 2.0 / scale;
        int tx0 = Math.floorDiv(Mth.floor(centerX - halfW), SIZE), tx1 = Math.floorDiv(Mth.floor(centerX + halfW), SIZE);
        int tz0 = Math.floorDiv(Mth.floor(centerZ - halfH), SIZE), tz1 = Math.floorDiv(Mth.floor(centerZ + halfH), SIZE);
        // Сначала дорисовываем плитки ближе к центру
        List<long[]> visible = new ArrayList<>();
        for (int tz = tz0; tz <= tz1; tz++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                visible.add(new long[]{tx, tz});
            }
        }
        double ctx = centerX / SIZE - 0.5, ctz = centerZ / SIZE - 0.5;
        visible.sort(Comparator.comparingDouble(t -> (t[0] - ctx) * (t[0] - ctx) + (t[1] - ctz) * (t[1] - ctz)));
        int budget = PAINT_BUDGET;

        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        if (rotation != 0) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        g.pose().scale(scale, scale, 1);
        Matrix4f matrix = g.pose().last().pose();
        for (long[] t : visible) {
            int tx = (int) t[0], tz = (int) t[1];
            Tile tile = TILES.computeIfAbsent(key(tx, tz), k -> new Tile());
            tile.lastUsed = frame;
            if (tile.dirty && budget > 0) {
                paint(tile, tx, tz);
                budget--;
            }
            if (tile.painted) {
                blitTile(matrix, tile.location, tx, tz, centerX, centerZ, alpha);
            }
        }
        g.pose().popPose();
        g.disableScissor();
        evict();
    }

    /**
     * Рисует плитку квадратом с углами в блоках относительно центра карты и с цветом в вершинах (так работает
     * прозрачность: GuiGraphics.blit цвет не применяет). Общий угол двух соседних плиток вычисляется одним и тем же
     * выражением и даёт одинаковое число, поэтому между плитками не остаётся щелей, как бы ни был сдвинут масштаб.
     */
    private static void blitTile(Matrix4f matrix, ResourceLocation texture, int tx, int tz, double centerX, double centerZ, float alpha) {
        float x0 = (float) (tx * SIZE - centerX), x1 = (float) ((tx + 1) * SIZE - centerX);
        float z0 = (float) (tz * SIZE - centerZ), z1 = (float) ((tz + 1) * SIZE - centerZ);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, x0, z0, 0).setUv(0, 0).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(matrix, x0, z1, 0).setUv(0, 1).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(matrix, x1, z1, 0).setUv(1, 1).setColor(1f, 1f, 1f, alpha);
        buffer.addVertex(matrix, x1, z0, 0).setUv(1, 0).setColor(1f, 1f, 1f, alpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void paint(Tile tile, int tx, int tz) {
        NativeImage image = tile.texture.getPixels();
        if (image == null) {
            return;
        }
        for (int j = 0; j < CHUNKS; j++) {
            for (int i = 0; i < CHUNKS; i++) {
                int[] colors = MapCache.chunk(dimension, tx * CHUNKS + i, tz * CHUNKS + j);
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        image.setPixelRGBA(i * 16 + lx, j * 16 + lz, colors == null ? 0 : colors[lz * 16 + lx]);
                    }
                }
            }
        }
        tile.texture.upload();
        tile.dirty = false;
        tile.painted = true;
    }

    /** Лишние плитки, которых давно не было на экране, освобождаются. */
    private static void evict() {
        if (TILES.size() <= MAX_TILES) {
            return;
        }
        List<Map.Entry<Long, Tile>> entries = new ArrayList<>(TILES.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue().lastUsed));
        for (int i = 0; i < entries.size() - MAX_TILES; i++) {
            Tile tile = entries.get(i).getValue();
            if (tile.lastUsed == frame) {
                break;
            }
            Minecraft.getInstance().getTextureManager().release(tile.location);
            TILES.remove(entries.get(i).getKey());
        }
    }

    public static void clear() {
        for (Tile tile : TILES.values()) {
            Minecraft.getInstance().getTextureManager().release(tile.location);
        }
        TILES.clear();
        dimension = null;
    }

    private static long key(int tx, int tz) {
        return (long) tx << 32 | (tz & 0xFFFFFFFFL);
    }

    private MapTiles() {}
}

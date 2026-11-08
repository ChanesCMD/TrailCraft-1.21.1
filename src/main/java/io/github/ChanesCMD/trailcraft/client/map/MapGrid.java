package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import java.util.Random;

/** Сетка чанков, слизневые чанки, спавн и граница мира на карте и миникарте. */
public final class MapGrid {
    private static final int CHUNK = 16;

    public static void draw(GuiGraphics g, int x, int y, int w, int h, double centerX, double centerZ,
                            float scale, float rotation, long seed, boolean minimap) {
        boolean grid = minimap ? ClientConfig.minimapChunkGrid() : ClientConfig.mapChunkGrid();
        if (!grid && !ClientConfig.mapSlimeChunks()) {
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        if (rotation != 0) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        g.pose().scale(scale, scale, 1);
        double half = Math.hypot(w, h) / 2.0 / scale + CHUNK;
        int minX = Mth.floor(centerX - half), maxX = Mth.ceil(centerX + half);
        int minZ = Mth.floor(centerZ - half), maxZ = Mth.ceil(centerZ + half);
        int left = Mth.floor(minX - centerX), right = Mth.ceil(maxX - centerX);
        int top = Mth.floor(minZ - centerZ), bottom = Mth.ceil(maxZ - centerZ);
        if (ClientConfig.mapSlimeChunks() && hasSeed()) {
            int c0x = Math.floorDiv(minX, CHUNK), c1x = Math.floorDiv(maxX, CHUNK);
            int c0z = Math.floorDiv(minZ, CHUNK), c1z = Math.floorDiv(maxZ, CHUNK);
            int color = 0x5500E000;
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) {
                    if (isSlime(seed, cx, cz)) {
                        int x0 = cx * CHUNK - Mth.floor(centerX), z0 = cz * CHUNK - Mth.floor(centerZ);
                        g.fill(x0, z0, x0 + CHUNK, z0 + CHUNK, color);
                    }
                }
            }
        }
        if (grid) {
            int step = ClientConfig.chunkGridStep().chunks * CHUNK;
            int alpha = Math.round(255 * ClientConfig.chunkGridOpacity() / 100f);
            int color = (alpha << 24) | 0xFFFFFF;
            int t = Math.max(1, Math.round(1f / scale));
            int firstX = Math.floorDiv(minX, step) * step;
            int firstZ = Math.floorDiv(minZ, step) * step;
            for (int wx = firstX; wx <= maxX; wx += step) {
                int lineX = wx - Mth.floor(centerX);
                g.fill(lineX, top, lineX + t, bottom, color);
            }
            for (int wz = firstZ; wz <= maxZ; wz += step) {
                int z = wz - Mth.floor(centerZ);
                g.fill(left, z, right, z + t, color);
            }
        }
        g.pose().popPose();
        g.disableScissor();
    }

    /** Граница мира и точка спавна — только на большой карте. */
    public static void drawLandmarks(GuiGraphics g, int x, int y, int w, int h, double centerX, double centerZ,
                                     float scale, float rotation, double[] border, int spawnX, int spawnZ) {
        if (!ClientConfig.mapWorldBorder() && !ClientConfig.mapSpawnChunk()) {
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        if (rotation != 0) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        g.pose().scale(scale, scale, 1);
        int cx = Mth.floor(centerX), cz = Mth.floor(centerZ);
        if (ClientConfig.mapSpawnChunk()) {
            int sx = Math.floorDiv(spawnX, CHUNK) * CHUNK - cx;
            int sz = Math.floorDiv(spawnZ, CHUNK) * CHUNK - cz;
            outline(g, sx, sz, sx + CHUNK, sz + CHUNK, 0xCCFFAA00, Math.max(1, Math.round(1f / scale)));
        }
        if (ClientConfig.mapWorldBorder() && border != null) {
            outline(g, Mth.floor(border[0]) - cx, Mth.floor(border[1]) - cz,
                    Mth.floor(border[2]) - cx, Mth.floor(border[3]) - cz,
                    0xCCFF3030, Math.max(1, Math.round(1f / scale)));
        }
        g.pose().popPose();
        g.disableScissor();
    }

    private static void outline(GuiGraphics g, int x0, int z0, int x1, int z1, int color, int t) {
        g.fill(x0, z0, x1, z0 + t, color);
        g.fill(x0, z1 - t, x1, z1, color);
        g.fill(x0, z0, x0 + t, z1, color);
        g.fill(x1 - t, z0, x1, z1, color);
    }

    /** Seed мира известен только в одиночной игре; в мультиплеере слизневые чанки не показываем. */
    public static long seed() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getSingleplayerServer() == null ? 0L : mc.getSingleplayerServer().overworld().getSeed();
    }

    public static boolean hasSeed() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    /** Ванильный алгоритм слизневого чанка. */
    public static boolean isSlime(long seed, int x, int z) {
        Random random = new Random(seed + (long) x * x * 4987142L + (long) x * 5947611L
                + (long) z * z * 4392871L + (long) z * 389711L);
        return random.nextInt(10) == 0;
    }

    public static ChunkPos chunk(double worldX, double worldZ) {
        return new ChunkPos(Mth.floor(worldX) >> 4, Mth.floor(worldZ) >> 4);
    }

    private MapGrid() {}
}

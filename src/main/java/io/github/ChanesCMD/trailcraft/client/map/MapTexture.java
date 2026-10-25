package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Текстура карты: пиксель — блок, вид сверху, север сверху. Цвета как у ванильной карты (MapColor): склон к северу
 * светлее, от севера темнее, вода темнее с глубиной. Незагруженные чанки чёрные. Общая для миникарты и большой карты.
 */
public final class MapTexture {
    private static final int MAX_DEPTH = 10;
    private final ResourceLocation location;
    private DynamicTexture texture;
    private int width, height;

    public MapTexture(String name) {
        this.location = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, name);
    }

    public ResourceLocation location() {
        return location;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Создаёт текстуру нужного размера (старая освобождается). */
    public void resize(int width, int height) {
        if (texture != null && this.width == width && this.height == height) {
            return;
        }
        close();
        this.width = width;
        this.height = height;
        texture = new DynamicTexture(width, height, true);
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    /** Рисует строки [fromRow, toRow) области, у которой левый верхний угол — блок (left, top). */
    public void paint(Level level, int left, int top, int fromRow, int toRow) {
        NativeImage image = texture == null ? null : texture.getPixels();
        if (image == null) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int row = Math.max(0, fromRow); row < Math.min(height, toRow); row++) {
            int z = top + row;
            for (int col = 0; col < width; col++) {
                int x = left + col;
                image.setPixelRGBA(col, row, color(level, pos, x, z, surface(level, x, z), surface(level, x, z - 1)));
            }
        }
    }

    public void upload() {
        if (texture != null) {
            texture.upload();
        }
    }

    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(location);
            texture = null;
        }
    }

    /** Высота верхнего блока или ниже мира, если чанк не загружен. */
    private static int surface(Level level, int x, int z) {
        return level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1 : level.getMinBuildHeight() - 1;
    }

    /** Цвет точки в формате NativeImage (ABGR); north — высота соседа с севера, для тени склона. */
    private static int color(Level level, BlockPos.MutableBlockPos pos, int x, int z, int height, int north) {
        if (height < level.getMinBuildHeight()) {
            return 0xFF000000;
        }
        pos.set(x, height, z);
        BlockState state = level.getBlockState(pos);
        for (int i = 0; i < MAX_DEPTH && state.getMapColor(level, pos) == MapColor.NONE && pos.getY() > level.getMinBuildHeight(); i++) {
            pos.move(0, -1, 0);
            state = level.getBlockState(pos);
        }
        MapColor color = state.getMapColor(level, pos);
        if (color == MapColor.NONE) {
            return 0xFF000000;
        }
        MapColor.Brightness brightness;
        if (!state.getFluidState().isEmpty()) {
            int depth = 0;
            while (depth < MAX_DEPTH && !level.getFluidState(pos.move(0, -1, 0)).isEmpty()) {
                depth++;
            }
            double shade = depth * 0.1 + ((x + z) & 1) * 0.2;
            brightness = shade < 0.5 ? MapColor.Brightness.HIGH : shade > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        } else {
            brightness = height > north ? MapColor.Brightness.HIGH : height < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        return color.calculateRGBColor(brightness);
    }
}

package io.github.ChanesCMD.trailcraft.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Текстура карты: пиксель — блок, вид сверху, север сверху. Трава, цветы и прочее без коллизии не рисуются —
 * видна земля под ними. Цвет — MapColor блока, у травы, листвы и воды — цвет биома; склоны плавно светлее
 * или темнее по разнице высот с северным соседом, вода темнее с глубиной. Незагруженные чанки прозрачные.
 */
public final class MapTexture {
    private static final int MAX_DEPTH = 12;
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

    /** Создаёт текстуру нужного размера (старая освобождается). @return true, если текстура новая (пустая) */
    public boolean resize(int width, int height) {
        if (texture != null && this.width == width && this.height == height) {
            return false;
        }
        close();
        this.width = width;
        this.height = height;
        texture = new DynamicTexture(width, height, true);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        return true;
    }

    /** Рисует прямоугольник пикселей [c0, c1) × [r0, r1) области, у которой левый верхний угол — блок (left, top). */
    public void paint(Level level, int left, int top, int c0, int r0, int c1, int r1) {
        NativeImage image = texture == null ? null : texture.getPixels();
        if (image == null) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int row = Math.max(0, r0); row < Math.min(height, r1); row++) {
            int z = top + row;
            for (int col = Math.max(0, c0); col < Math.min(width, c1); col++) {
                int x = left + col;
                image.setPixelRGBA(col, row, color(level, pos, x, z, surface(level, pos, x, z), surface(level, pos, x, z - 1)));
            }
        }
    }

    /** Сдвигает картинку: пиксель (c, r) получает прежний (c + dx, r + dz); открывшиеся края прозрачные. */
    public void shift(int dx, int dz) {
        NativeImage image = texture == null ? null : texture.getPixels();
        if (image == null) {
            return;
        }
        int[] old = new int[width * height];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                old[r * width + c] = image.getPixelRGBA(c, r);
            }
        }
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int oc = c + dx, or = r + dz;
                image.setPixelRGBA(c, r, oc >= 0 && oc < width && or >= 0 && or < height ? old[or * width + oc] : 0);
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

    /** Блок видимой поверхности в столбце (x, z) — тот, что нарисован на карте; null, если чанк не загружен. */
    public static BlockPos surfaceAt(Level level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y = surface(level, pos, x, z);
        return y < level.getMinBuildHeight() ? null : new BlockPos(x, y, z);
    }

    /** Высота видимой поверхности (без травы и цветов) или ниже мира, если чанк не загружен. */
    private static int surface(Level level, BlockPos.MutableBlockPos pos, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return level.getMinBuildHeight() - 1;
        }
        pos.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        for (int i = 0; i < MAX_DEPTH && pos.getY() > level.getMinBuildHeight() && skipped(level, pos, level.getBlockState(pos)); i++) {
            pos.move(0, -1, 0);
        }
        return pos.getY();
    }

    /** Не рисуется: пустота и всё без коллизии и жидкости (трава, цветы, факелы…), кроме снега. */
    private static boolean skipped(Level level, BlockPos pos, BlockState state) {
        if (state.getMapColor(level, pos) == MapColor.NONE) {
            return true;
        }
        return state.getFluidState().isEmpty() && !state.is(Blocks.SNOW) && state.getCollisionShape(level, pos).isEmpty();
    }

    /** Цвет точки в формате NativeImage (ABGR); north — высота поверхности северного соседа. */
    private static int color(Level level, BlockPos.MutableBlockPos pos, int x, int z, int height, int north) {
        if (height < level.getMinBuildHeight()) {
            return 0;
        }
        pos.set(x, height, z);
        BlockState state = level.getBlockState(pos);
        if (skipped(level, pos, state)) {
            return 0xFF000000;
        }
        int rgb;
        float shade;
        if (state.getFluidState().is(FluidTags.WATER)) {
            rgb = BiomeColors.getAverageWaterColor(level, pos);
            int depth = 0;
            BlockPos.MutableBlockPos below = pos.mutable();
            while (depth < MAX_DEPTH && level.getFluidState(below.move(0, -1, 0)).is(FluidTags.WATER)) {
                depth++;
            }
            shade = 0.85f - depth * 0.03f + ((x + z) & 1) * 0.02f;
        } else {
            if (state.is(Blocks.GRASS_BLOCK)) {
                rgb = BiomeColors.getAverageGrassColor(level, pos);
            } else if (state.is(BlockTags.LEAVES) && !state.is(Blocks.CHERRY_LEAVES) && !state.is(Blocks.AZALEA_LEAVES)
                    && !state.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
                rgb = BiomeColors.getAverageFoliageColor(level, pos);
            } else {
                rgb = state.getMapColor(level, pos).col;
            }
            shade = 1f + Mth.clamp((height - north) * 0.12f, -0.3f, 0.2f);
        }
        int r = Mth.clamp((int) ((rgb >> 16 & 0xFF) * shade), 0, 255);
        int g = Mth.clamp((int) ((rgb >> 8 & 0xFF) * shade), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * shade), 0, 255);
        return 0xFF000000 | b << 16 | g << 8 | r;
    }
}

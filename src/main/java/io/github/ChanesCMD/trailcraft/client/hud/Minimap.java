package io.github.ChanesCMD.trailcraft.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Миникарта 66×66 (ТЗ, раздел 4): вид сверху 64×64 блока вокруг игрока, север сверху, цвета как у ванильной карты
 * (MapColor, склоны светлее/темнее, вода темнее с глубиной). Незагруженные места чёрные. Перерисовывается
 * раз в UPDATE_TICKS тиков. Игрок — белая точка с чёрной обводкой и выступом в сторону взгляда.
 */
public final class Minimap {
    public static final int SIZE = 66;
    private static final int MAP = SIZE - 2, UPDATE_TICKS = 10, MAX_DEPTH = 10;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "minimap");
    private static DynamicTexture texture;
    private static long lastUpdate = Long.MIN_VALUE;

    public static void render(GuiGraphics g, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (texture == null) {
            texture = new DynamicTexture(MAP, MAP, true);
            mc.getTextureManager().register(TEXTURE, texture);
        }
        long time = player.level().getGameTime();
        if (time - lastUpdate >= UPDATE_TICKS || time < lastUpdate) {
            lastUpdate = time;
            update(player.level(), player.blockPosition());
        }
        UiDraw.frame(g, x, y, SIZE, SIZE, 0xFF373737, 0xFFFFFFFF, 0xFF000000);
        g.blit(TEXTURE, x + 1, y + 1, 0, 0, MAP, MAP, MAP, MAP);

        int cx = x + 1 + MAP / 2, cy = y + 1 + MAP / 2;
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        int dx = player.getDirection().getStepX(), dz = player.getDirection().getStepZ();
        g.fill(cx - 1 + dx * 2, cy - 1 + dz * 2, cx + 1 + dx * 2, cy + 1 + dz * 2, 0xFFFFFFFF);
    }

    private static void update(Level level, BlockPos center) {
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int left = center.getX() - MAP / 2, top = center.getZ() - MAP / 2;
        for (int px = 0; px < MAP; px++) {
            int previous = surface(level, left + px, top - 1);
            for (int pz = 0; pz < MAP; pz++) {
                int x = left + px, z = top + pz;
                int height = surface(level, x, z);
                image.setPixelRGBA(px, pz, color(level, pos, x, z, height, previous));
                previous = height;
            }
        }
        texture.upload();
    }

    /** Высота верхнего блока или минимум мира, если чанк не загружен. */
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

    private Minimap() {}
}

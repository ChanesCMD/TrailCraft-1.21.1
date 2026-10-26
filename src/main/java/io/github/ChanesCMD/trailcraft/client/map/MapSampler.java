package io.github.ChanesCMD.trailcraft.client.map;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.ClientGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Атлас запоминает местность: пока он в рюкзаке, загруженные чанки вокруг игрока снимаются в MapCache —
 * сначала ещё не исследованные, а 7×7 чанков у игрока обновляются каждую секунду; ближние чанки раньше дальних.
 * По CHUNKS_PER_TICK чанков за тик,
 * чтобы не было фризов. Цвет — как на виде сверху: трава, цветы и прочее без коллизии не рисуются, у травы, листвы
 * и воды цвет биома, склоны светлее или темнее по разнице высот с северным соседом, вода темнее с глубиной.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class MapSampler {
    private static final int CHUNKS_PER_TICK = 8, SCAN_TICKS = 20, SAVE_TICKS = 1200, MAX_DEPTH = 12;
    /** Чанки в этом радиусе от игрока (7×7) обновляются каждую секунду: там меняются постройки, деревья, вода. */
    private static final int REFRESH_RADIUS = 3;
    private static final Set<Long> QUEUE = new LinkedHashSet<>();
    private static ClientLevel queueLevel;
    private static int ticks;

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        MapCache.open();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MapCache.close();
        MapTiles.clear();
        QUEUE.clear();
        queueLevel = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        MapCache.tick();
        if (++ticks % SAVE_TICKS == 0) {
            MapCache.save();
        }
        if (!ClientGear.hasAtlas()) {
            return;
        }
        if (level != queueLevel) {
            QUEUE.clear();
            queueLevel = level;
        }
        ResourceLocation dimension = level.dimension().location();
        ChunkPos center = player.chunkPosition();
        if (ticks % SCAN_TICKS == 0) {
            // Всё загруженное (радиус прорисовки, но не больше, чем сервер прислал): сначала ближние чанки
            int radius = mc.options.getEffectiveRenderDistance();
            List<long[]> wanted = new ArrayList<>();
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int cx = center.x + dx, cz = center.z + dz;
                    boolean near = Math.abs(dx) <= REFRESH_RADIUS && Math.abs(dz) <= REFRESH_RADIUS;
                    if ((near || !MapCache.hasChunk(dimension, cx, cz)) && level.hasChunk(cx, cz)) {
                        wanted.add(new long[]{ChunkPos.asLong(cx, cz), (long) dx * dx + (long) dz * dz});
                    }
                }
            }
            wanted.sort(Comparator.comparingLong(entry -> entry[1]));
            for (long[] entry : wanted) {
                QUEUE.add(entry[0]);
            }
        }
        Iterator<Long> it = QUEUE.iterator();
        for (int done = 0; done < CHUNKS_PER_TICK && it.hasNext(); ) {
            long key = it.next();
            it.remove();
            int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
            if (level.hasChunk(cx, cz)) {
                MapCache.putChunk(dimension, cx, cz, sample(level, cx, cz));
                done++;
            }
        }
    }

    /** Цвета 16×16 загруженного чанка. */
    private static int[] sample(Level level, int cx, int cz) {
        int[] colors = new int[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = cx * 16 + lx, z = cz * 16 + lz;
                int height = surface(level, pos, x, z), north = surface(level, pos, x, z - 1);
                colors[lz * 16 + lx] = color(level, pos, x, z, height, north < level.getMinBuildHeight() ? height : north);
            }
        }
        return colors;
    }

    /** Блок видимой поверхности в столбце (x, z) — тот, что рисуется на карте; null, если чанк не загружен. */
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

    /** Цвет точки в формате NativeImage (ABGR); north — высота поверхности северного соседа. 0 — нет данных. */
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

    private MapSampler() {}
}

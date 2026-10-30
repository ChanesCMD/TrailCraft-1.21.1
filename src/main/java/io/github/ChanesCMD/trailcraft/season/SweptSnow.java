package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Столбцы, где метлой смели снег (item/BroomItem): снегопад туда больше не кладёт снег (mixin/ServerLevelMixin).
 * Хранятся у чанка (ModAttachments.SWEPT_SNOW), ключ — позиция столбца с y = 0 (BlockPos.asLong).
 * Shift + ПКМ метлой снимает метку ({@link #unmark}), метла в руке подсвечивает такие столбцы (BroomEvents, client/SweptSnowOverlay).
 */
public final class SweptSnow {
    public static void mark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.getData(ModAttachments.SWEPT_SNOW).add(key(pos))) {
            chunk.setUnsaved(true);
        }
    }

    /** Снимает метку: снег в этом столбце снова может падать. false — столбец и не был сметён. */
    public static boolean unmark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.hasData(ModAttachments.SWEPT_SNOW) && chunk.getData(ModAttachments.SWEPT_SNOW).remove(key(pos))) {
            chunk.setUnsaved(true);
            return true;
        }
        return false;
    }

    public static boolean isSwept(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(ModAttachments.SWEPT_SNOW) && chunk.getData(ModAttachments.SWEPT_SNOW).contains(key(pos));
    }

    /**
     * Y, в которой в столбце ложится снег: над кроной дерева, если сверху лежит листва, иначе над землёй
     * (листва проходима и в MOTION_BLOCKING не входит, поэтому ванильный снегопад её пропускает).
     */
    public static int surface(Level level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (level.getBlockState(new BlockPos(x, top - 1, z)).getBlock() instanceof LeavesBlock) {
            return top;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** Сметённые столбцы в радиусе radius блоков от center (не больше max), с высотой, на которой лёг бы снег. */
    public static List<BlockPos> near(ServerLevel level, BlockPos center, int radius, int max) {
        List<BlockPos> out = new ArrayList<>();
        long r2 = (long) radius * radius;
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null || !chunk.hasData(ModAttachments.SWEPT_SNOW)) {
                    continue;
                }
                for (long key : chunk.getData(ModAttachments.SWEPT_SNOW)) {
                    BlockPos column = BlockPos.of(key);
                    long dx = column.getX() - center.getX(), dz = column.getZ() - center.getZ();
                    if (dx * dx + dz * dz > r2) {
                        continue;
                    }
                    out.add(new BlockPos(column.getX(), surface(level, column.getX(), column.getZ()), column.getZ()));
                    if (out.size() >= max) {
                        return out;
                    }
                }
            }
        }
        return out;
    }

    private static long key(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), 0, pos.getZ());
    }

    private SweptSnow() {}
}

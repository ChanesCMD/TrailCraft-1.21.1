package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Столбцы, где метлой смели снег (item/BroomItem): снегопад туда больше не кладёт снег (mixin/ServerLevelMixin).
 * Хранятся у чанка (ModAttachments.SWEPT_SNOW), ключ — позиция столбца с y = 0 (BlockPos.asLong).
 */
public final class SweptSnow {
    public static void mark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.getData(ModAttachments.SWEPT_SNOW).add(key(pos))) {
            chunk.setUnsaved(true);
        }
    }

    public static boolean isSwept(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(ModAttachments.SWEPT_SNOW) && chunk.getData(ModAttachments.SWEPT_SNOW).contains(key(pos));
    }

    private static long key(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), 0, pos.getZ());
    }

    private SweptSnow() {}
}

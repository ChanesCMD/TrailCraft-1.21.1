package io.github.ChanesCMD.trailcraft.collapse;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Породы и почвы, поставленные игроком: они не обваливаются (обваливается только природное, вскрытое игроком).
 * Хранятся у чанка (ModAttachments.PLACED_BLOCKS), позиция забывается, когда блок ломают.
 */
public final class PlacedBlocks {
    public static void mark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.getData(ModAttachments.PLACED_BLOCKS).add(pos.asLong())) {
            chunk.setUnsaved(true);
        }
    }

    public static boolean isPlaced(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(ModAttachments.PLACED_BLOCKS) && chunk.getData(ModAttachments.PLACED_BLOCKS).contains(pos.asLong());
    }

    public static void forget(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.hasData(ModAttachments.PLACED_BLOCKS) && chunk.getData(ModAttachments.PLACED_BLOCKS).remove(pos.asLong())) {
            chunk.setUnsaved(true);
        }
    }

    private PlacedBlocks() {}
}

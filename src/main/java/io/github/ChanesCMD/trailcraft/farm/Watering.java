package io.github.ChanesCMD.trailcraft.farm;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

/**
 * Полив лейкой: до какого времени мира грядка влажная. Хранится у чанка (ModAttachments.WATERED_BEDS), отсчёт — по
 * времени суток мира (dayTime), поэтому 2 игровых суток — это 2 смены дня и ночи при любой длине суток в конфиге.
 */
public final class Watering {
    public static final long TICKS_PER_DAY = 24000;

    /** Полить грядку: влажная Farm.WATERED_DAYS суток с этого момента. */
    public static void water(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<Long, Long> beds = chunk.getData(ModAttachments.WATERED_BEDS);
        long now = level.getDayTime();
        beds.values().removeIf(until -> until <= now);  // заодно забыть высохшие и сломанные грядки
        beds.put(pos.asLong(), now + Farm.WATERED_DAYS * TICKS_PER_DAY);
        chunk.setUnsaved(true);
    }

    /** Грядка ещё влажная от полива. */
    public static boolean isWatered(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ModAttachments.WATERED_BEDS)) {
            return false;
        }
        Long until = chunk.getData(ModAttachments.WATERED_BEDS).get(pos.asLong());
        return until != null && until > level.getDayTime();
    }

    /** Забыть полив (грядку затоптали или сломали). */
    public static void forget(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk.hasData(ModAttachments.WATERED_BEDS) && chunk.getData(ModAttachments.WATERED_BEDS).remove(pos.asLong()) != null) {
            chunk.setUnsaved(true);
        }
    }

    private Watering() {}
}

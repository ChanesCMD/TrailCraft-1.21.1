package io.github.ChanesCMD.trailcraft.client;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Лежит ли снег на дереве (клиент). Зимой крона белая только если на ней есть хотя бы один слой снега: дерево — это
 * связная листва (в том числе по диагонали) и брёвна между ней; один слой снега на любой её клетке — белое всё дерево. Результат на клетку листвы
 * кешируется; кеш сбрасывается, когда меняется снег или листва (LevelRendererMixin → {@link #onBlockChanged}), сменился
 * сезон или число загруженных чанков (SeasonColors.onClientTick). Когда на дереве появляется или пропадает снег, секции с
 * его листвой перестраиваются, чтобы перекрасилась вся крона, а не только кусок рядом со снегом.
 */
public final class TreeSnow {
    /** Сколько блоков дерева (листва + брёвна) просматриваем: у слипшихся крон лес не считается одним деревом бесконечно. */
    private static final int LIMIT = 2048;
    private static final Map<Long, Boolean> CACHE = new ConcurrentHashMap<>();

    private record Scan(boolean snowy, LongArrayList leaves) {}

    private TreeSnow() {}

    public static void clear() {
        CACHE.clear();
    }

    /** Есть ли снег на дереве, которому принадлежит эта листва. */
    public static boolean isSnowy(BlockPos leaf) {
        Boolean known = CACHE.get(leaf.asLong());
        if (known != null) {
            return known;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        Scan scan = scan(level, leaf);
        for (long l : scan.leaves()) {
            CACHE.put(l, scan.snowy());
        }
        CACHE.putIfAbsent(leaf.asLong(), scan.snowy());
        return scan.snowy();
    }

    /** Обходит дерево от клетки листвы: есть ли снег над любой его листвой и какая листва у него есть. */
    private static Scan scan(ClientLevel level, BlockPos start) {
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongArrayList leaves = new LongArrayList();
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        boolean snowy = false;
        seen.add(start.asLong());
        queue.enqueue(start.asLong());
        while (!queue.isEmpty() && seen.size() < LIMIT) {
            long packed = queue.dequeueLong();
            cur.set(packed);
            boolean isLeaf = level.getBlockState(cur).getBlock() instanceof LeavesBlock;
            if (isLeaf) {
                leaves.add(packed);
                if (level.getBlockState(next.setWithOffset(cur, Direction.UP)).is(Blocks.SNOW)) {
                    snowy = true;
                }
            }
            // листва держится за соседей и по диагонали (висящие на краю кроны листья), ствол — только гранями
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                        if (dist == 0 || dist > 1 && !isLeaf) {
                            continue;
                        }
                        next.set(cur.getX() + dx, cur.getY() + dy, cur.getZ() + dz);
                        long n = next.asLong();
                        if (!seen.contains(n) && isTreePart(level.getBlockState(next))) {
                            seen.add(n);
                            queue.enqueue(n);
                        }
                    }
                }
            }
        }
        return new Scan(snowy, leaves);
    }

    private static boolean isTreePart(BlockState state) {
        // тонкие брёвна мода и любые брёвна-столбы (в том числе обтёсанные) не входят в #minecraft:logs
        return state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LOGS) || state.is(ModTags.THIN_LOG_BLOCKS)
                || state.getBlock() instanceof RotatedPillarBlock;
    }

    /** Вызывается при любом изменении блока в клиентском мире (LevelRendererMixin.blockChanged). */
    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        boolean snow = oldState.is(Blocks.SNOW) != newState.is(Blocks.SNOW);
        boolean leaves = (oldState.getBlock() instanceof LeavesBlock) != (newState.getBlock() instanceof LeavesBlock);
        if (!snow && !leaves) {
            return;
        }
        CACHE.clear();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (!snow || level == null || !SeasonColors.snowPossible()) {
            return;
        }
        BlockPos leaf = pos.below();
        if (!(level.getBlockState(leaf).getBlock() instanceof LeavesBlock)) {
            return;
        }
        LongOpenHashSet sections = new LongOpenHashSet();
        for (long l : scan(level, leaf).leaves()) {
            sections.add(SectionPos.asLong(BlockPos.of(l)));
        }
        for (long s : sections) {
            mc.levelRenderer.setSectionDirty(SectionPos.x(s), SectionPos.y(s), SectionPos.z(s));
        }
    }
}

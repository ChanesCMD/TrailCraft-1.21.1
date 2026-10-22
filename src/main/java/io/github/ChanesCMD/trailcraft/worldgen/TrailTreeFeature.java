package io.github.ChanesCMD.trailcraft.worldgen;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.github.ChanesCMD.trailcraft.block.ThinLogBlock;
import io.github.ChanesCMD.trailcraft.init.ModWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Генератор деревьев TrailCraft. Молодые деревья — маленькие, целиком из тонких брёвен;
 * старые — выше, ствол из обычных брёвен, ветви из тонких. Форма кроны зависит от породы.
 * Землю под деревом не ставит.
 */
public class TrailTreeFeature extends Feature<TrailTreeConfig> {
    public TrailTreeFeature() {
        super(TrailTreeConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<TrailTreeConfig> ctx) {
        TrailTreeConfig cfg = ctx.config();
        ModWood.WoodSet set = null;
        for (ModWood.WoodSet s : ModWood.SETS) if (s.id.equals(cfg.wood())) set = s;
        if (set == null) return false;
        WorldGenLevel level = ctx.level();
        BlockPos base = ctx.origin();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        boolean old = ctx.random().nextFloat() < cfg.oldChance();
        Builder b = new Builder(level, ctx.random(), set, base, old);
        if (!b.grow(cfg.shape())) return false;
        // при генерации мира деревья не должны касаться друг друга (саженцы игрока можно сажать вплотную)
        if (!(level instanceof ServerLevel) && b.touchesOtherTree()) return false;
        b.commit();
        return true;
    }

    /** Собирает дерево в памяти, потом ставит все блоки разом и выставляет листве расстояние до ствола. */
    private static final class Builder {
        final WorldGenLevel level;
        final RandomSource r;
        final ModWood.WoodSet set;
        final BlockPos base;
        final boolean old;
        final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        final Set<BlockPos> leaves = new HashSet<>();

        Builder(WorldGenLevel level, RandomSource r, ModWood.WoodSet set, BlockPos base, boolean old) {
            this.level = level; this.r = r; this.set = set; this.base = base; this.old = old;
        }

        // ---------- примитивы

        boolean free(BlockPos p) {
            BlockState s = level.getBlockState(p);
            return s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.LEAVES);
        }

        void fullLog(BlockPos p, Direction.Axis axis) {
            if (free(p) || logs.containsKey(p)) {
                logs.put(p.immutable(), set.log.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
                leaves.remove(p);
            }
        }

        void thinLog(BlockPos p) {
            if (logs.containsKey(p)) return; // толстое бревно не заменяем тонким
            if (free(p)) {
                logs.put(p.immutable(), set.thinLog.get().defaultBlockState());
                leaves.remove(p);
            }
        }

        void leaf(BlockPos p) {
            if (logs.containsKey(p)) return;
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES)) leaves.add(p.immutable());
        }

        /** Шапка листвы-эллипсоид с неровным краем. */
        void blob(BlockPos c, double rx, double ry, double rz) {
            int ix = Mth.ceil(rx), iy = Mth.ceil(ry), iz = Mth.ceil(rz);
            for (int dx = -ix; dx <= ix; dx++)
                for (int dy = -iy; dy <= iy; dy++)
                    for (int dz = -iz; dz <= iz; dz++) {
                        double d = sq(dx / rx) + sq(dy / ry) + sq(dz / rz);
                        if (d <= 1.0 && !(d > 0.7 && r.nextFloat() < 0.35)) leaf(c.offset(dx, dy, dz));
                    }
        }

        /** Ветка от a до b: путь, где соседние блоки касаются гранями (так тонкие брёвна соединяются). */
        void branch(BlockPos a, BlockPos b, boolean full) {
            int x = a.getX(), y = a.getY(), z = a.getZ();
            Direction.Axis axis = Direction.Axis.Y;
            put(new BlockPos(x, y, z), full, axis);
            int guard = 0;
            while ((x != b.getX() || y != b.getY() || z != b.getZ()) && guard++ < 64) {
                int dx = b.getX() - x, dy = b.getY() - y, dz = b.getZ() - z;
                double tx = Math.abs(dx), ty = Math.abs(dy) * 1.15, tz = Math.abs(dz); // при равенстве сперва идём вверх
                if (ty >= tx && ty >= tz) { y += Integer.signum(dy); axis = Direction.Axis.Y; }
                else if (tx >= tz) { x += Integer.signum(dx); axis = Direction.Axis.X; }
                else { z += Integer.signum(dz); axis = Direction.Axis.Z; }
                put(new BlockPos(x, y, z), full, axis);
            }
        }

        void put(BlockPos p, boolean full, Direction.Axis axis) {
            if (full) fullLog(p, axis); else thinLog(p);
        }

        void trunk(int h, boolean full) {
            for (int y = 0; y < h; y++) put(base.above(y), full, Direction.Axis.Y);
        }

        /** Корни у старого дерева: обычные брёвна по бокам у земли. */
        void roots(int chance) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = base.relative(d);
                if (r.nextInt(100) < chance && level.getBlockState(p.below()).isSolid()) fullLog(p, d.getAxis());
            }
        }

        BlockPos dir(BlockPos from, double angle, double len, int rise) {
            return from.offset((int) Math.round(Math.cos(angle) * len), rise, (int) Math.round(Math.sin(angle) * len));
        }

        static double sq(double v) { return v * v; }

        boolean trunkFree(int h) {
            for (int y = 0; y < h; y++) if (!free(base.above(y))) return false;
            return true;
        }

        // ---------- формы

        boolean grow(String shape) {
            return switch (shape) {
                case "cone" -> cone();
                case "tall" -> tall();
                case "pine" -> pine();
                case "weeping" -> weeping();
                case "palm" -> palm();
                case "acacia" -> acacia();
                case "sakura" -> sakura();
                default -> round();
            };
        }

        /** Круглая крона: раскидистые ветви, на концах шапки листвы (дуб, каштан, орех, клён...). */
        boolean round() {
            int h = old ? 6 + r.nextInt(4) : 3 + r.nextInt(2);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            BlockPos top = base.above(h - 1);
            if (old) {
                roots(60);
                int n = 4 + r.nextInt(3);
                double a0 = r.nextDouble() * Math.PI * 2;
                for (int i = 0; i < n; i++) {
                    BlockPos from = base.above(h / 2 + r.nextInt(Math.max(1, h / 2)));
                    BlockPos end = dir(from, a0 + i * Math.PI * 2 / n + r.nextDouble() * 0.5, 3 + r.nextInt(2), 2 + r.nextInt(2));
                    branch(from, end, false);
                    blob(end, 2.6, 2.0, 2.6);
                }
                blob(top.above(), 3.2, 2.4, 3.2);
            } else {
                BlockPos end = dir(base.above(h - 2), r.nextDouble() * Math.PI * 2, 1, 1);
                branch(base.above(h - 2), end, false);
                blob(top.above(), 2.0, 1.8, 2.0);
            }
            return true;
        }

        /** Конус ярусами (ель, пихта, туя, кедр). */
        boolean cone() {
            int h = old ? 11 + r.nextInt(6) : 4 + r.nextInt(3);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            if (old) roots(40);
            double rmax = old ? 4.2 : 2.0;
            int start = old ? 3 : 1;
            for (int y = start; y <= h; y++) {
                double t = (double) (y - start) / (h - start + 1);
                double rad = rmax * (1 - t) + 0.6;
                boolean tier = (y - start) % 2 == 0;
                if (old && tier && rad > 1.5) {
                    for (Direction d : Direction.Plane.HORIZONTAL) branch(base.above(y), base.above(y).relative(d, (int) rad - 1), false);
                }
                double rr = tier ? rad : rad * 0.6;
                for (int dx = -3 - (int) rr; dx <= 3 + rr; dx++)
                    for (int dz = -3 - (int) rr; dz <= 3 + rr; dz++)
                        if (sq(dx) + sq(dz) <= rr * rr && !(sq(dx) + sq(dz) > rr * rr * 0.7 && r.nextFloat() < 0.3))
                            leaf(base.offset(dx, y, dz));
            }
            leaf(base.above(h)); leaf(base.above(h + 1));
            return true;
        }

        /** Высокая стройная (берёза, осина, тополь, эвкалипт). */
        boolean tall() {
            int h = old ? 10 + r.nextInt(5) : 4 + r.nextInt(3);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            if (old) {
                roots(30);
                for (int i = 0; i < 4; i++) {
                    BlockPos from = base.above(h / 2 + r.nextInt(h / 2));
                    BlockPos end = dir(from, r.nextDouble() * Math.PI * 2, 2, 1);
                    branch(from, end, false);
                    blob(end, 1.6, 1.6, 1.6);
                }
                blob(base.above(h - h / 4), 2.4, h * 0.32, 2.4);
            } else {
                blob(base.above(h - 1), 1.5, 2.2, 1.5);
            }
            return true;
        }

        /** Сосна: высокий голый ствол, наверху плоские «зонтики» на ветвях. */
        boolean pine() {
            int h = old ? 12 + r.nextInt(5) : 4 + r.nextInt(3);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            BlockPos top = base.above(h - 1);
            if (old) {
                roots(50);
                int n = 3 + r.nextInt(2);
                double a0 = r.nextDouble() * Math.PI * 2;
                for (int i = 0; i < n; i++) {
                    BlockPos from = base.above(h - 4 + r.nextInt(3));
                    BlockPos end = dir(from, a0 + i * Math.PI * 2 / n, 3, 2);
                    branch(from, end, false);
                    blob(end.above(), 2.8, 1.0, 2.8);
                }
                blob(top.above(), 2.5, 1.2, 2.5);
            } else {
                blob(top, 1.8, 1.5, 1.8);
                leaf(top.above());
            }
            return true;
        }

        /** Плакучая (ива, верба): широкая крона, с краёв свисают пряди листвы. */
        boolean weeping() {
            int h = old ? 5 + r.nextInt(3) : 3 + r.nextInt(2);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            BlockPos top = base.above(h - 1);
            double rx = old ? 4.2 : 2.3;
            if (old) {
                roots(70);
                int n = 5 + r.nextInt(2);
                for (int i = 0; i < n; i++) {
                    BlockPos end = dir(top, i * Math.PI * 2 / n + r.nextDouble() * 0.4, 3, 1);
                    branch(top.below(), end, false);
                }
            }
            blob(top.above(), rx, old ? 2.2 : 1.5, rx);
            // пряди: с нижнего края кроны вниз через воздух
            for (int i = 0; i < (old ? 22 : 8); i++) {
                double a = r.nextDouble() * Math.PI * 2;
                BlockPos p = top.offset((int) Math.round(Math.cos(a) * (rx - 0.5)), 0, (int) Math.round(Math.sin(a) * (rx - 0.5)));
                int len = 2 + r.nextInt(old ? 4 : 2);
                for (int k = 0; k < len; k++) leaf(p.below(k));
            }
            return true;
        }

        /** Пальма: изогнутый ствол, наверху листья-веера. */
        boolean palm() {
            int h = old ? 8 + r.nextInt(5) : 3 + r.nextInt(2);
            if (!trunkFree(3)) return false;
            double lean = r.nextDouble() * Math.PI * 2;
            BlockPos prev = base;
            put(base, old, Direction.Axis.Y);
            for (int y = 1; y < h; y++) {
                double off = 2.6 * sq((double) y / h) * (old ? 1 : 0.5);
                BlockPos p = base.offset((int) Math.round(Math.cos(lean) * off), y, (int) Math.round(Math.sin(lean) * off));
                branch(prev, p, old); // по граням, чтобы ствол не рвался
                prev = p;
            }
            BlockPos top = prev;
            leaf(top.above());
            int fronds = old ? 7 : 5, len = old ? 4 : 2;
            for (int i = 0; i < fronds; i++) {
                double a = i * Math.PI * 2 / fronds + r.nextDouble() * 0.3;
                for (int k = 1; k <= len; k++) {
                    int droop = k >= len - 1 ? -1 : 0;
                    leaf(top.offset((int) Math.round(Math.cos(a) * k), 1 + droop, (int) Math.round(Math.sin(a) * k)));
                }
            }
            return true;
        }

        /** Акация: ствол раздваивается, плоская широкая крона. */
        boolean acacia() {
            int h = old ? 4 + r.nextInt(2) : 2 + r.nextInt(2);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            BlockPos fork = base.above(h - 1);
            int forks = old ? 2 + r.nextInt(2) : 1;
            double a0 = r.nextDouble() * Math.PI * 2;
            for (int i = 0; i < forks; i++) {
                BlockPos end = dir(fork, a0 + i * Math.PI * 2 / forks, old ? 3 : 1, old ? 3 : 2);
                branch(fork, end, old);
                blob(end.above(), old ? 3.6 : 2.2, 0.9, old ? 3.6 : 2.2);
            }
            if (old) roots(40);
            return true;
        }

        /** Сакура и сирень: невысокий ствол, широкие ветви, пышные облака листвы. */
        boolean sakura() {
            int h = old ? 4 + r.nextInt(2) : 2 + r.nextInt(2);
            if (!trunkFree(h)) return false;
            trunk(h, old);
            BlockPos top = base.above(h - 1);
            int n = old ? 4 : 2;
            double a0 = r.nextDouble() * Math.PI * 2;
            for (int i = 0; i < n; i++) {
                BlockPos end = dir(top, a0 + i * Math.PI * 2 / n + r.nextDouble() * 0.6, old ? 4 : 2, old ? 2 : 1);
                branch(top, end, false);
                blob(end.above(), old ? 3.0 : 1.8, old ? 1.8 : 1.3, old ? 3.0 : 1.8);
            }
            blob(top.above(2), old ? 2.6 : 1.6, 1.6, old ? 2.6 : 1.6);
            if (old) roots(50);
            return true;
        }

        /** Касается ли будущее дерево (гранью или тем же блоком) чужих брёвен или листвы. */
        boolean touchesOtherTree() {
            Set<BlockPos> own = new HashSet<>(logs.keySet());
            own.addAll(leaves);
            for (BlockPos p : own) {
                if (isTree(level.getBlockState(p))) return true;
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (!own.contains(n) && isTree(level.getBlockState(n))) return true;
                }
            }
            return false;
        }

        static boolean isTree(BlockState s) {
            return s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES);
        }

        // ---------- установка

        void commit() {
            for (var e : logs.entrySet()) level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_CLIENTS);
            // расстояние листвы до ближайшего бревна, как у ванильных деревьев; слишком далёкую листву не ставим
            Map<BlockPos, Integer> dist = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            for (BlockPos p : logs.keySet()) { dist.put(p, 0); queue.add(p); }
            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                int d = dist.get(p);
                if (d >= 6) continue;
                for (Direction dir : Direction.values()) {
                    BlockPos n = p.relative(dir);
                    if (leaves.contains(n) && !dist.containsKey(n)) { dist.put(n, d + 1); queue.add(n); }
                }
            }
            BlockState leafState = set.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
            for (BlockPos p : leaves) {
                Integer d = dist.get(p);
                if (d != null) level.setBlock(p, leafState.setValue(LeavesBlock.DISTANCE, d), Block.UPDATE_CLIENTS);
            }
            // тонкие брёвна соединяем в самом конце, когда стоят и брёвна, и листва
            for (var e : logs.entrySet()) {
                BlockState s = e.getValue();
                if (s.getBlock() instanceof ThinLogBlock) {
                    BlockPos p = e.getKey();
                    level.setBlock(p, ThinLogBlock.withConnections(level, p, s), Block.UPDATE_CLIENTS);
                    // соседей могут поставить и позже (другое дерево, соседний чанк): тогда игра пересчитает
                    // форму сама, когда чанк догенерируется (как у заборов в постройках)
                    if (!(level instanceof ServerLevel)) level.getChunk(p).markPosForPostprocessing(p);
                }
            }
        }
    }
}

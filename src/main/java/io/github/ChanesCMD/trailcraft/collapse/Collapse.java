package io.github.ChanesCMD.trailcraft.collapse;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.OreBlock;
import io.github.ChanesCMD.trailcraft.block.SupportBlock;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Обвалы, как в TFC, но с предупреждением. Обваливается только природное и только когда игрок что-то выкопал рядом:
 * сломал породу, руду или крепь — с шансом {@link #TRIGGER_CHANCE} ищется нависающая порода (#trailcraft:can_collapse,
 * под ней пусто) в радиусе {@link #MIN_RADIUS}…{@link #MIN_RADIUS} + {@link #RADIUS_VARIANCE} − 1, не поставленная
 * игроком (PlacedBlocks) и не под крепью (SupportBlock). Найденное 2–5 с сыплет пылью с потолка и трещит, потом падает:
 * порода — своим гравием, руда — сама собой; блоки над ней — с шансом {@link #PROPAGATE_CHANCE} каждый, столбом.
 * Камни бьют по {@link #DAMAGE_PER_BLOCK} за блок падения, не больше {@link #MAX_DAMAGE} (шлем снимает четверть).
 * Почва (#trailcraft:collapsible_soils) осыпается сразу, как песок, если игрок выкопал блок под ней, и никого не бьёт.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class Collapse {
    public static final float TRIGGER_CHANCE = 0.1f;
    public static final int MIN_RADIUS = 3;
    public static final int RADIUS_VARIANCE = 6;
    public static final float PROPAGATE_CHANCE = 0.55f;
    /** Сколько тиков потолок трещит перед обвалом. */
    public static final int MIN_DELAY = 40, MAX_DELAY = 100;
    public static final float DAMAGE_PER_BLOCK = 4.0f;
    public static final int MAX_DAMAGE = 12;
    /** Дальше всего держит железная крепь: ±6 в стороны, ±3 по высоте. */
    private static final int SUPPORT_REACH_H = 6, SUPPORT_REACH_V = 3;

    private static final Map<Level, LevelCollapses> LEVELS = new IdentityHashMap<>();
    private static final Map<Block, BlockState> RUBBLE = new HashMap<>();

    private static final class LevelCollapses {
        /** Сломанное игроком в этот тик: проверяется в конце тика, когда блока уже нет. */
        final List<BlockPos> broken = new ArrayList<>(), triggers = new ArrayList<>();
        final List<Pending> pending = new ArrayList<>();
    }

    /** Обвал, который ещё трещит: starts — нижние блоки (с них сыплется пыль), blocks — всё, что упадёт. */
    private record Pending(List<BlockPos> starts, List<BlockPos> blocks, long at) {}

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && event.getLevel() instanceof ServerLevel level && isTracked(event.getPlacedBlock())) {
            PlacedBlocks.mark(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (isTracked(event.getState())) {
            PlacedBlocks.forget(level, pos);
        }
        Player player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        LevelCollapses state = LEVELS.computeIfAbsent(level, l -> new LevelCollapses());
        state.broken.add(pos.immutable());
        if (event.getState().is(ModTags.CAN_COLLAPSE) || event.getState().getBlock() instanceof SupportBlock) {
            state.triggers.add(pos.immutable());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LevelCollapses state = LEVELS.get(level);
        if (state == null) {
            return;
        }
        for (BlockPos pos : state.broken) {
            if (level.isEmptyBlock(pos) || FallingBlock.isFree(level.getBlockState(pos))) {
                dropSoil(level, pos.above());
            }
        }
        state.broken.clear();
        for (BlockPos pos : state.triggers) {
            if (level.random.nextFloat() < TRIGGER_CHANCE) {
                Pending pending = find(level, pos);
                if (pending != null) {
                    state.pending.add(pending);
                    BlockPos start = pending.starts.get(0);
                    level.playSound(null, start, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.5f, 0.5f);
                }
            }
        }
        state.triggers.clear();
        long now = level.getGameTime();
        state.pending.removeIf(pending -> {
            if (now >= pending.at) {
                fall(level, pending);
                return true;
            }
            warn(level, pending);
            return false;
        });
        if (state.pending.isEmpty()) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        LEVELS.remove(event.getLevel());
    }

    // === Порода ===

    /** Ищет нависающую породу вокруг pos; null — обваливаться нечему. */
    private static Pending find(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;
        int radius = MIN_RADIUS + random.nextInt(RADIUS_VARIANCE);
        BlockPos min = pos.offset(-radius, -1, -radius), max = pos.offset(radius, radius, radius);
        if (!level.hasChunksAt(min, max)) {
            return null;
        }
        List<BlockPos> supports = supports(level, min, max);
        List<BlockPos> starts = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            int dx = p.getX() - pos.getX(), dz = p.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= radius * radius && canCollapse(level, p, supports)
                    && FallingBlock.isFree(level.getBlockState(p.below()))) {
                starts.add(p.immutable());
            }
        }
        if (starts.isEmpty()) {
            return null;
        }
        Set<BlockPos> blocks = new LinkedHashSet<>(starts);
        for (BlockPos start : starts) {
            BlockPos above = start.above();
            while (above.getY() <= max.getY() && random.nextFloat() < PROPAGATE_CHANCE && canCollapse(level, above, supports)) {
                blocks.add(above);
                above = above.above();
            }
        }
        return new Pending(starts, List.copyOf(blocks), level.getGameTime() + MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY + 1));
    }

    /** Пыль с потолка и треск, пока обвал не случился. */
    private static void warn(ServerLevel level, Pending pending) {
        RandomSource random = level.random;
        for (int i = 0; i < 3; i++) {
            BlockPos p = pending.starts.get(random.nextInt(pending.starts.size()));
            BlockState state = level.getBlockState(p);
            if (!state.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                        p.getX() + random.nextDouble(), p.getY() - 0.05, p.getZ() + random.nextDouble(), 1, 0, 0, 0, 0);
            }
        }
        if (random.nextInt(12) == 0) {
            BlockPos p = pending.starts.get(random.nextInt(pending.starts.size()));
            level.playSound(null, p, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0f, 0.5f + random.nextFloat() * 0.3f);
        }
    }

    private static void fall(ServerLevel level, Pending pending) {
        BlockPos min = pending.blocks.get(0), max = min;
        for (BlockPos p : pending.blocks) {
            min = BlockPos.min(min, p);
            max = BlockPos.max(max, p);
        }
        if (!level.hasChunksAt(min, max)) {
            return;
        }
        // крепь могли поставить, пока трещало
        List<BlockPos> supports = supports(level, min, max);
        List<BlockPos> sorted = new ArrayList<>(pending.blocks);
        sorted.sort(Comparator.comparingInt(BlockPos::getY));
        BlockPos first = null;
        for (BlockPos p : sorted) {
            BlockState state = level.getBlockState(p);
            if (canCollapse(level, p, supports) && FallingBlock.isFree(level.getBlockState(p.below()))) {
                FallingBlockEntity.fall(level, p, rubble(state)).setHurtsEntities(DAMAGE_PER_BLOCK, MAX_DAMAGE);
                if (first == null) {
                    first = p;
                }
            }
        }
        if (first != null) {
            level.playSound(null, first, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
            level.playSound(null, first, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 2.0f, 0.6f);
        }
    }

    private static boolean canCollapse(ServerLevel level, BlockPos pos, List<BlockPos> supports) {
        return level.getBlockState(pos).is(ModTags.CAN_COLLAPSE) && !PlacedBlocks.isPlaced(level, pos) && !supported(level, pos, supports);
    }

    /** Во что превращается упавшая порода: в свой гравий (trailcraft:<порода>_gravel); руда падает сама собой. */
    private static BlockState rubble(BlockState state) {
        if (state.getBlock() instanceof OreBlock) {
            return state;
        }
        return RUBBLE.computeIfAbsent(state.getBlock(), block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            Block gravel = BuiltInRegistries.BLOCK.get(id.withSuffix("_gravel"));
            return gravel == Blocks.AIR ? block.defaultBlockState() : gravel.defaultBlockState();
        });
    }

    // === Почва ===

    /** Почва над выкопанным блоком осыпается столбом, если под ней пусто. */
    private static void dropSoil(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !isLooseSoil(level, pos) || !FallingBlock.isFree(level.getBlockState(pos.below()))) {
            return;
        }
        List<BlockPos> column = new ArrayList<>();
        for (BlockPos p = pos; level.isLoaded(p) && isLooseSoil(level, p); p = p.above()) {
            column.add(p);
        }
        List<BlockPos> supports = supports(level, pos, column.get(column.size() - 1));
        for (BlockPos p : column) {
            if (supported(level, p, supports)) {
                return;  // крепь держит этот блок, а с ним и всё, что выше
            }
            FallingBlockEntity.fall(level, p, level.getBlockState(p));
        }
    }

    private static boolean isLooseSoil(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModTags.COLLAPSIBLE_SOILS) && !PlacedBlocks.isPlaced(level, pos);
    }

    // === Крепь ===

    /** Вся крепь, которая может держать что-то в коробке min…max. */
    private static List<BlockPos> supports(ServerLevel level, BlockPos min, BlockPos max) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(min.offset(-SUPPORT_REACH_H, -SUPPORT_REACH_V, -SUPPORT_REACH_H),
                max.offset(SUPPORT_REACH_H, SUPPORT_REACH_V, SUPPORT_REACH_H))) {
            if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof SupportBlock) {
                found.add(p.immutable());
            }
        }
        return found;
    }

    private static boolean supported(ServerLevel level, BlockPos pos, List<BlockPos> supports) {
        for (BlockPos support : supports) {
            if (level.getBlockState(support).getBlock() instanceof SupportBlock block && block.covers(support, pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTracked(BlockState state) {
        return state.is(ModTags.CAN_COLLAPSE) || state.is(ModTags.COLLAPSIBLE_SOILS);
    }

    private Collapse() {}
}

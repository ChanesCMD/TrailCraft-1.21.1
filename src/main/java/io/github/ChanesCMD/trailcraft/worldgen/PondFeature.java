package io.github.ChanesCMD.trailcraft.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Озерцо на равнине: 1–5 блоков воды вровень с землёй, глубиной в блок (вода заменяет верхний блок почвы).
 * Клетка берётся, только если под ней и вокруг неё твёрдая земля на той же высоте, — вода никуда не вытекает.
 * Ставится на шаге lakes (до травы и деревьев), см. neoforge/biome_modifier/plains_ponds.json.
 */
public class PondFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_SIZE = 1, MAX_SIZE = 5;

    public PondFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public PondFeature() {
        this(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos start = ctx.origin().below();
        if (!ground(level, start)) {
            return false;
        }
        int size = MIN_SIZE + random.nextInt(MAX_SIZE - MIN_SIZE + 1);
        Set<BlockPos> pond = new LinkedHashSet<>();
        pond.add(start);
        for (int tries = 0; pond.size() < size && tries < size * 8; tries++) {
            List<BlockPos> cells = new ArrayList<>(pond);
            BlockPos next = cells.get(random.nextInt(cells.size())).relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (!pond.contains(next) && ground(level, next)) {
                pond.add(next);
            }
        }
        // убираем клетки, из которых вода вытекла бы вбок (рядом не земля и не озерцо), пока все не будут закрыты
        boolean changed = true;
        while (changed && !pond.isEmpty()) {
            changed = pond.removeIf(cell -> {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos side = cell.relative(dir);
                    if (!pond.contains(side) && !wall(level, side)) {
                        return true;
                    }
                }
                return false;
            });
        }
        if (pond.isEmpty()) {
            return false;
        }
        for (BlockPos cell : pond) {
            level.setBlock(cell, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Верхний блок земли: твёрдый, над ним пусто, под ним твёрдо, высота как у соседей (по карте высот). */
    private static boolean ground(WorldGenLevel level, BlockPos pos) {
        if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()) != pos.getY() + 1) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return solid(state) && level.getBlockState(pos.above()).isAir() && solid(level.getBlockState(pos.below()));
    }

    /** Берег: твёрдый блок, который удержит воду. */
    private static boolean wall(WorldGenLevel level, BlockPos pos) {
        return solid(level.getBlockState(pos));
    }

    private static boolean solid(BlockState state) {
        return state.canOcclude() && state.getFluidState().isEmpty() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }
}

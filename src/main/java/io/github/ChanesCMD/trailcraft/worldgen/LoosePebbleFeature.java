package io.github.ChanesCMD.trailcraft.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.ChanesCMD.trailcraft.block.PebbleBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Камешек на поверхности, как в TerraFirmaCraft: порода берётся из столбца под ним, верхние породы встречаются чаще,
 * глубинные — реже. Иногда вместо камешка появляется кремень. Под водой камешек ставится затопленным.
 */
public class LoosePebbleFeature extends Feature<NoneFeatureConfiguration> {
    /** Доля кремня среди камешков: достаточно часто, чтобы долго не искать. */
    private static final float FLINT_CHANCE = 0.12f;
    /** На сколько блоков вниз ищем породу. */
    private static final int SEARCH_DEPTH = 128;
    /** Насколько быстро падает шанс породы с глубиной (в блоках). */
    private static final double DEPTH_FALLOFF = 16.0;

    /** Генерация мира идёт в нескольких потоках, поэтому ConcurrentHashMap (обычный HashMap там может сломаться). */
    private static final Map<Block, Block> ROCK_TO_PEBBLE = new ConcurrentHashMap<>();

    public LoosePebbleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static Block pebbleFor(Block rock) {
        return ROCK_TO_PEBBLE.computeIfAbsent(rock, r -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(r);
            ResourceLocation pebble = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_pebble");
            return BuiltInRegistries.BLOCK.containsKey(pebble) ? BuiltInRegistries.BLOCK.get(pebble) : Blocks.AIR;
        });
    }

    /**
     * Собирает породы в столбце под точкой (до SEARCH_DEPTH блоков) и выбирает одну случайно:
     * чем ближе порода к поверхности, тем больше её вес (exp(-глубина / DEPTH_FALLOFF)).
     */
    private static Block pickRock(WorldGenLevel level, BlockPos start, RandomSource random) {
        Map<Block, Integer> firstDepth = new LinkedHashMap<>();
        BlockPos.MutableBlockPos p = start.mutable();
        for (int depth = 0; depth < SEARCH_DEPTH && p.getY() > level.getMinBuildHeight(); depth++, p.move(Direction.DOWN)) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModTags.ROCKS)) firstDepth.putIfAbsent(s.getBlock(), depth);
        }
        double total = 0;
        for (int d : firstDepth.values()) total += Math.exp(-d / DEPTH_FALLOFF);
        double roll = random.nextDouble() * total;
        for (var e : firstDepth.entrySet()) {
            roll -= Math.exp(-e.getValue() / DEPTH_FALLOFF);
            if (roll <= 0) return pebbleFor(e.getKey());
        }
        return Blocks.AIR;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos pos = ctx.origin();
        BlockState here = level.getBlockState(pos);
        boolean water = here.is(Blocks.WATER) && here.getFluidState().isSource();
        if (!here.isAir() && !water) return false;
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS) || ground.is(ModTags.THIN_LOG_BLOCKS)) return false;

        Block pebble = Blocks.AIR;
        if (ctx.random().nextFloat() < FLINT_CHANCE) {
            pebble = ModBlocks.FLINT_PEBBLE.get();
        } else {
            pebble = pickRock(level, below, ctx.random());
        }
        if (pebble == Blocks.AIR) return false;
        BlockState state = pebble.defaultBlockState();
        if (state.hasProperty(PebbleBlock.WATERLOGGED)) state = state.setValue(PebbleBlock.WATERLOGGED, water);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }
}

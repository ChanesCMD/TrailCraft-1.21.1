package io.github.ChanesCMD.trailcraft.worldgen;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Заросли длинной водоросли: от дна вверх ставится столб стеблей, сверху — верхушка.
 * Работает как ванильный келп, но высота своя и блоки свои.
 * Ставится на дне океанов, рек и берегов (neoforge/biome_modifier/plants_tall_seaweed.json).
 */
public class TallSeaweedFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_HEIGHT = 4, MAX_HEIGHT = 18;

    public TallSeaweedFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        BlockPos pos = new BlockPos(origin.getX(), floor, origin.getZ());
        if (!level.getBlockState(pos).is(Blocks.WATER)) {
            return false;
        }
        BlockState stem = ModBlocks.TALL_SEAWEED_PLANT.get().defaultBlockState();
        BlockState head = ModBlocks.TALL_SEAWEED.get().defaultBlockState();
        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        int placed = 0;
        for (int i = 0; i <= height; i++) {
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER)
                    && stem.canSurvive(level, pos)) {
                if (i == height) {
                    level.setBlock(pos, head.setValue(GrowingPlantHeadBlock.AGE, random.nextInt(4) + 20), 2);
                    placed++;
                } else {
                    level.setBlock(pos, stem, 2);
                }
            } else if (i > 0) {
                // вода кончилась раньше — верхушка на последнем блоке стебля
                BlockPos below = pos.below();
                if (head.canSurvive(level, below)
                        && !level.getBlockState(below.below()).is(ModBlocks.TALL_SEAWEED.get())) {
                    level.setBlock(below, head.setValue(GrowingPlantHeadBlock.AGE, random.nextInt(4) + 20), 2);
                    placed++;
                }
                break;
            }
            pos = pos.above();
        }
        return placed > 0;
    }
}

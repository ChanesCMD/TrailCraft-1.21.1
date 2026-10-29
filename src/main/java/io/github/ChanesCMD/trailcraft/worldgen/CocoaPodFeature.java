package io.github.ChanesCMD.trailcraft.worldgen;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Стручки какао на стволах пальм и эбеновых деревьев (#trailcraft:cocoa_logs): в чанке {@link #COLUMNS} случайных
 * столбцов, в каждом сверху вниз ищется бревно; на свободную сторону ствола с шансом {@link #CHANCE} — стручок
 * случайной стадии. Ставится после деревьев (шаг растительности, модификатор биома добавляет в конец).
 */
public class CocoaPodFeature extends Feature<NoneFeatureConfiguration> {
    private static final int COLUMNS = 24;
    private static final float CHANCE = 0.35f;

    public CocoaPodFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;
        for (int i = 0; i < COLUMNS; i++) {
            int x = origin.getX() + random.nextInt(16), z = origin.getZ() + random.nextInt(16);
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            for (int y = top; y > top - 24 && y > level.getMinBuildHeight(); y--) {
                BlockPos log = new BlockPos(x, y, z);
                if (!level.getBlockState(log).is(ModTags.COCOA_LOGS)) {
                    continue;
                }
                // не только у кроны: на несколько блоков ниже по стволу
                for (int down = random.nextInt(5); down > 0 && level.getBlockState(log.below()).is(ModTags.COCOA_LOGS); down--) {
                    log = log.below();
                }
                if (random.nextFloat() < CHANCE) {
                    Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    BlockPos pod = log.relative(side);
                    BlockState state = ModBlocks.COCOA.get().defaultBlockState()
                            .setValue(CocoaBlock.FACING, side.getOpposite()).setValue(CocoaBlock.AGE, random.nextInt(3));
                    if (level.isEmptyBlock(pod) && state.canSurvive(level, pod)) {
                        level.setBlock(pod, state, 2);
                        placed = true;
                    }
                }
                break;
            }
        }
        return placed;
    }
}

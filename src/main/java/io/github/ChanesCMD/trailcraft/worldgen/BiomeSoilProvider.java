package io.github.ChanesCMD.trailcraft.worldgen;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

/**
 * Земля под деревом азалии (dirt_provider): корнистый зём той почвы, на которой дерево растёт, — в бурозёме
 * корнистый бурозём, в чернозёме чернозём, в жёлтозёме жёлтозём. Сам BlockStateProvider мира не знает (ему дают только
 * random и pos), поэтому почву под деревом ищет mixin/TrunkPlacerMixin и зовёт {@link #stateAt(LevelReader, BlockPos)}.
 */
public class BiomeSoilProvider extends BlockStateProvider {
    public static final MapCodec<BiomeSoilProvider> CODEC = MapCodec.unit(BiomeSoilProvider::new);
    /** Насколько глубоко искать почву: сверху дёрн ({@link io.github.ChanesCMD.trailcraft.block.TurfBlock}), под ним почва. */
    private static final int DEPTH = 4;

    @Override
    protected BlockStateProviderType<?> type() {
        return ModFeatures.BIOME_SOIL.get();
    }

    /** Без мира почву не определить; игра сюда не обращается (см. TrunkPlacerMixin) — пусть будет ванильная земля. */
    @Override
    public BlockState getState(RandomSource random, BlockPos pos) {
        return Blocks.ROOTED_DIRT.defaultBlockState();
    }

    /** Корнистый зём почвы, на которой стоит дерево; если почвы нет (песок, снег, камень) — ванильная корнистая земля. */
    public BlockState stateAt(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos below = pos.mutable();
        for (int i = 0; i < DEPTH; i++) {
            BlockState rooted = rootedFor(level.getBlockState(below).getBlock());
            if (rooted != null) {
                return rooted;
            }
            below.move(Direction.DOWN);
        }
        return Blocks.ROOTED_DIRT.defaultBlockState();
    }

    private static BlockState rootedFor(Block block) {
        if (block == ModBlocks.BUROZEM.get()) {
            return ModBlocks.ROOTED_BUROZEM.get().defaultBlockState();
        }
        if (block == ModBlocks.CHERNOZEM.get()) {
            return ModBlocks.ROOTED_CHERNOZEM.get().defaultBlockState();
        }
        if (block == ModBlocks.ZHELTOZEM.get()) {
            return ModBlocks.ROOTED_ZELTOZEM.get().defaultBlockState();
        }
        return null;
    }
}

package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.worldgen.BiomeSoilProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * Земля под деревом азалии зависит от биома: ванильный {@link TrunkPlacer#setDirtAt} ставит то, что вернул
 * dirt_provider, а у провайдера нет мира — только random и pos. Здесь мир есть, поэтому для BiomeSoilProvider
 * земля выбирается по почве под деревом (worldgen/BiomeSoilProvider).
 */
@Mixin(TrunkPlacer.class)
public abstract class TrunkPlacerMixin {
    @Inject(method = "setDirtAt", at = @At("HEAD"), cancellable = true)
    private static void trailcraft$biomeSoil(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                             RandomSource random, BlockPos pos, TreeConfiguration config, CallbackInfo ci) {
        if (!(config.dirtProvider instanceof BiomeSoilProvider soil) || !(level instanceof LevelReader reader)) {
            return;
        }
        BlockState old = reader.getBlockState(pos);
        if (!old.onTreeGrow(reader, blockSetter, random, pos, config) && (config.forceDirt || !isDirt(old))) {
            blockSetter.accept(pos, soil.stateAt(reader, pos));
        }
        ci.cancel();
    }

    /** Как у ванильного TrunkPlacer.isDirt: трава и мицелий — не земля, их дерево заменяет всегда. */
    private static boolean isDirt(BlockState state) {
        return Feature.isDirt(state) && !state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.MYCELIUM);
    }
}

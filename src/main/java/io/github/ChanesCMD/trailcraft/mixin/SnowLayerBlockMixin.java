package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Где стало тепло для дождя, снег медленно тает по слою (season/Seasons.melts, meltChance). Листва проходима (пустая
 * коллизия, mixin/LeavesBlockMixin), и по ванильному правилу снег на ней не удержался бы — разрешаем ему лежать на листве,
 * от этого зависит белая крона зимой (client/TreeSnow).
 */
@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void trailcraft$melt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (random.nextFloat() < Seasons.meltChance(level) && Seasons.melts(level, pos)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers > 1) {
                level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1));
            } else {
                level.removeBlock(pos, false);
            }
            ci.cancel();
        }
    }

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void trailcraft$onLeaves(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(true);
        }
    }
}

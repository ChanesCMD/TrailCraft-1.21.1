package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Лёд, намёрзший зимой, где стало тепло для дождя, медленно тает обратно в воду (season/Seasons.melts, meltChance). */
@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void trailcraft$melt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (random.nextFloat() < Seasons.meltChance(level) && Seasons.melts(level, pos)) {
            melt(state, level, pos);
            ci.cancel();
        }
    }
}

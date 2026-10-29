package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.LeafParticles;
import io.github.ChanesCMD.trailcraft.client.weather.WeatherParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Погода частицами: в дождь с листвы капает своя капля (WeatherParticles.leafDrip), а не ванильная синяя
 * (в 1.21.1 animateTick листвы только её и спавнит). С сакуры и ивы падают лепестки и листья (LeafParticles).
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockClientMixin {
    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void trailcraft$leafDrip(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        LeafParticles.animate(state, level, pos, random);
        if (WeatherParticles.enabled()) {
            WeatherParticles.leafDrip(level, pos, random);
            ci.cancel();
        }
    }
}

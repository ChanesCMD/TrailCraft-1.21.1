package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.TreeSnow;
import io.github.ChanesCMD.trailcraft.client.weather.WeatherParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Погода частицами (client/weather/WeatherParticles): ванильные полосы дождя и снега не рисуются, а в tickRain
 * не спавнятся ванильные брызги и дымок (их заменяют свои). Звук дождя из tickRain остаётся.
 * Снег появился на дереве или пропал (blockChanged) — client/TreeSnow сбрасывает кеш и перестраивает крону.
 * Облака рисуются дальше ванильного: площадка buildClouds расширена с 8 клеток (768 блоков) до 20 (1920 блоков).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /** Полуширина площадки облаков в клетках по 8 единиц (96 блоков). Ваниль — 3 (768 блоков), тут 9 (1920). */
    private static final int TRAILCRAFT_CLOUD_CELLS = 9;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void trailcraft$particleWeather(CallbackInfo ci) {
        if (WeatherParticles.enabled()) {
            ci.cancel();
        }
    }

    // Облака: в buildClouds клетки идут циклом -3..4, здесь границы заменяются на -N..N+1.
    // require = 0: если ванильные константы вдруг изменятся, облака просто останутся ванильными.
    @ModifyConstant(method = "buildClouds", require = 0, constant = @Constant(intValue = -3))
    private int trailcraft$cloudFieldStart(int original) {
        return -TRAILCRAFT_CLOUD_CELLS;
    }

    @ModifyConstant(method = "buildClouds", require = 0, constant = @Constant(intValue = 4))
    private int trailcraft$cloudFieldEnd(int original) {
        return TRAILCRAFT_CLOUD_CELLS + 1;
    }

    @Redirect(method = "tickRain", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void trailcraft$noVanillaRainParticles(ClientLevel level, ParticleOptions particle,
                                                   double x, double y, double z, double xd, double yd, double zd) {
        if (!WeatherParticles.enabled()) {
            level.addParticle(particle, x, y, z, xd, yd, zd);
        }
    }

    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void trailcraft$treeSnow(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        TreeSnow.onBlockChanged(pos, oldState, newState);
    }
}

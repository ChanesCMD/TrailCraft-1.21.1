package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.weather.WeatherParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Погода частицами (client/weather/WeatherParticles): ванильные полосы дождя и снега не рисуются, а в tickRain
 * не спавнятся ванильные брызги и дымок (их заменяют свои). Звук дождя из tickRain остаётся.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void trailcraft$particleWeather(CallbackInfo ci) {
        if (WeatherParticles.enabled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "tickRain", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void trailcraft$noVanillaRainParticles(ClientLevel level, ParticleOptions particle,
                                                   double x, double y, double z, double xd, double yd, double zd) {
        if (!WeatherParticles.enabled()) {
            level.addParticle(particle, x, y, z, xd, yd, zd);
        }
    }
}

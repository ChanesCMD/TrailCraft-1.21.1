package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.weather.WeatherParticles;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Погода частицами (client/weather/WeatherParticles): ванильные полосы дождя и снега не рисуются.
 * Звук дождя и ванильный tickRain остаются.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void trailcraft$particleWeather(CallbackInfo ci) {
        if (WeatherParticles.enabled()) {
            ci.cancel();
        }
    }
}

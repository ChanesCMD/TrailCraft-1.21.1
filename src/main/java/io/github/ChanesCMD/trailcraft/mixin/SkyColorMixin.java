package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.SeasonSky;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Сезонный оттенок неба (client/SeasonSky). */
@Mixin(Level.class)
public abstract class SkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void trailcraft$seasonSky(Vec3 cameraPos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide()) {
            cir.setReturnValue(SeasonSky.sky(cir.getReturnValue(), level));
        }
    }
}

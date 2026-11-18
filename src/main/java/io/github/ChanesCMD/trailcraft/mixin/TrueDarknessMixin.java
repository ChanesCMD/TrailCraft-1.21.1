package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.TrueDarkness;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Тёмная ночь (client/TrueDarkness): ночью небесный свет притухает, неосвещённые места становятся чёрными. */
@Mixin(ClientLevel.class)
public abstract class TrueDarknessMixin {
    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void trailcraft$trueDarkness(float partialTick, CallbackInfoReturnable<Float> cir) {
        float factor = TrueDarkness.skyLightFactor((ClientLevel) (Object) this);
        if (factor < 1f) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}

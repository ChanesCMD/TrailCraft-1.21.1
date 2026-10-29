package io.github.ChanesCMD.trailcraft.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import io.github.ChanesCMD.trailcraft.pipe.LavaPipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Труба Create, по которой течёт лава, нагревается и прогорает (pipe/LavaPipes). */
@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class FluidTransportBehaviourMixin {
    @Unique
    private int trailcraft$heat;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void trailcraft$lavaHeat(CallbackInfo ci) {
        trailcraft$heat = LavaPipes.tick((FluidTransportBehaviour) (Object) this, trailcraft$heat);
        if (trailcraft$heat < 0) {
            trailcraft$heat = 0;
            ci.cancel();
        }
    }
}

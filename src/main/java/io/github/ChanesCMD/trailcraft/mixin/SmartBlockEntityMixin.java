package io.github.ChanesCMD.trailcraft.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.ChanesCMD.trailcraft.pipe.LavaPipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Шкив шланга, носик, слив, интерфейс жидкостей и бак Create с лавой внутри нагреваются и прогорают (pipe/LavaPipes). */
@Mixin(value = SmartBlockEntity.class, remap = false)
public abstract class SmartBlockEntityMixin {
    @Unique
    private int trailcraft$heat;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void trailcraft$lavaHeat(CallbackInfo ci) {
        trailcraft$heat = LavaPipes.tickContainer((SmartBlockEntity) (Object) this, trailcraft$heat);
        if (trailcraft$heat < 0) {
            trailcraft$heat = 0;
            ci.cancel();
        }
    }
}

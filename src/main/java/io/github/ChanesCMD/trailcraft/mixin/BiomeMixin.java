package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Зимой биомы холоднее (season/Seasons): снег вместо дождя, замерзание воды. Сдвиг — после кэша температуры биома. */
@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void trailcraft$seasonTemperature(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float offset = Seasons.temperatureOffset();
        if (offset != 0f) {
            cir.setReturnValue(cir.getReturnValueF() + offset);
        }
    }
}

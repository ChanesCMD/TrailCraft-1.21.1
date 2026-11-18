package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.TrueDarkness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Тёмная ночь (client/TrueDarkness): подменяем skyDarken в лайтмапе, чтобы ночью неосвещённые места были чёрными. */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Redirect(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F"))
    private float trailcraft$trueDarkness(ClientLevel level, float partialTick) {
        return TrueDarkness.skyDarken(level, level.getSkyDarken(partialTick));
    }

    /**
     * notGamma поднимает тёмные значения (1 - (1 - v)^4): из-за него даже 3 % яркости выглядят как 11 %.
     * Ночью возвращаем значение без подъёма — неосвещённое становится по-настоящему тёмным,
     * а яркие клетки (факелы, лава) почти не меняются: у них notGamma и так ~1.
     */
    @Inject(method = "notGamma", at = @At("RETURN"), cancellable = true)
    private void trailcraft$notGamma(float value, CallbackInfoReturnable<Float> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float strength = TrueDarkness.strength(level);
        if (strength > 0f) {
            cir.setReturnValue(Mth.lerp(strength, cir.getReturnValueF(), value));
        }
    }
}

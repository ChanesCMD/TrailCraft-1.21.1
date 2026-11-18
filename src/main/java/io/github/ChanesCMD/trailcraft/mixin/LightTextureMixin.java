package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.client.TrueDarkness;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Тёмная ночь (client/TrueDarkness): подменяем skyDarken в лайтмапе, чтобы ночью неосвещённые места были чёрными. */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Redirect(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F"))
    private float trailcraft$trueDarkness(ClientLevel level, float partialTick) {
        return TrueDarkness.skyDarken(level, level.getSkyDarken(partialTick));
    }
}

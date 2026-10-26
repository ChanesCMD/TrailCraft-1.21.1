package io.github.ChanesCMD.trailcraft.mixin;

import mezz.jei.api.gui.buttons.IIconButtonController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Убирает у JEI кнопку-шестерёнку (она открывала настройки JEI или писала в чат папку конфигурации): всё это
 * теперь делает кнопка JEI в левом верхнем углу экрана инвентаря TrailCraft (client/gui/CornerButtons.java).
 * Кнопке JEI не дают получить размер: у неё пустая область, она не рисуется и не нажимается.
 * Без JEI миксин не применяется (@Pseudo), а с другой версией JEI — тоже пропускается (require = 0), игра не падает.
 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.elements.IconButton", remap = false)
public abstract class JeiIconButtonMixin {
    @Shadow
    @Final
    private IIconButtonController controller;

    @Inject(method = "updateBounds", at = @At("HEAD"), cancellable = true, require = 0)
    private void trailcraft$keepConfigButtonHidden(CallbackInfo ci) {
        if (controller != null && controller.getClass().getName().endsWith("ConfigButtonController")) {
            ci.cancel();
        }
    }
}

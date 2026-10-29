package io.github.ChanesCMD.trailcraft.mixin;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Котёл Create складывает нагрев горелок под ним, отбрасывая дробную часть у каждой: угольная горелка (нагрев
 * CoalBurnerBlock.BOILER_HEAT = 1/4 горелки всполоха) так ничего бы не давала. Дробный нагрев здесь суммируется
 * отдельно: 4 горящие угольные горелки — один уровень нагрева, 1–3 — пассивный нагрев.
 */
@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerDataMixin {
    @Shadow
    public boolean passiveHeat;
    @Shadow
    public int activeHeat;
    @Unique
    private float trailcraft$partialHeat;

    @Inject(method = "updateTemperature", at = @At("HEAD"))
    private void trailcraft$resetPartial(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        trailcraft$partialHeat = 0;
    }

    @Redirect(method = "updateTemperature", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/api/boiler/BoilerHeater;findHeat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float trailcraft$collectPartial(Level level, BlockPos pos, BlockState state) {
        float heat = BoilerHeater.findHeat(level, pos, state);
        if (heat > 0 && heat < 1) {
            trailcraft$partialHeat += heat;
            return BoilerHeater.NO_HEAT;
        }
        return heat;
    }

    /** После обхода горелок, до того как котёл решает, пассивный ли нагрев. */
    @Inject(method = "updateTemperature", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;passiveHeat:Z", opcode = 180, ordinal = 1))
    private void trailcraft$applyPartial(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        int whole = (int) (trailcraft$partialHeat + 1.0e-4f);
        activeHeat += whole;
        if (trailcraft$partialHeat > 0 && whole == 0) {
            passiveHeat = true;
        }
    }
}

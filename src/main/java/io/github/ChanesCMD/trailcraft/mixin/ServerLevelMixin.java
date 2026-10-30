package io.github.ChanesCMD.trailcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.ChanesCMD.trailcraft.season.SweptSnow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Снегопад не кладёт снег туда, где его смели метлой (season/SweptSnow); вода там замерзает как обычно. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "tickPrecipitation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean trailcraft$noSnowOnSwept(Biome biome, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return original.call(biome, level, pos) && !SweptSnow.isSwept((ServerLevel) (Object) this, pos);
    }
}

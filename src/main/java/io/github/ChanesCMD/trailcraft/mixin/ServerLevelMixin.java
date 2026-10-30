package io.github.ChanesCMD.trailcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.ChanesCMD.trailcraft.block.SnowCover;
import io.github.ChanesCMD.trailcraft.season.SweptSnow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Снегопад не кладёт снег туда, где его смели метлой (season/SweptSnow); вода там замерзает как обычно.
 * Листва проходима и в MOTION_BLOCKING не попадает, поэтому снег падал бы мимо крон на землю: если над столбцом
 * крона и на ней снег может лечь, снегопад кладёт его на листву (SweptSnow.surface).
 * Если вместо воздуха в клетке стоит мелкий блок со свойством SNOW (трава, камешек, куст — SnowCover), снег ложится в неё слоем, а блок торчит сверху.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "tickPrecipitation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean trailcraft$noSnowOnSwept(Biome biome, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        ServerLevel self = (ServerLevel) (Object) this;
        BlockState at = self.getBlockState(pos);
        if (SnowCover.accepts(at)) {
            SnowCover.fall(self, biome, pos, at);
            return false;  // ванильный снег в эту клетку не кладём: он заменил бы блок
        }
        return original.call(biome, level, pos) && !SweptSnow.isSwept(self, pos);
    }

    @WrapOperation(method = "tickPrecipitation", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos trailcraft$snowOnLeaves(ServerLevel level, Heightmap.Types types, BlockPos pos, Operation<BlockPos> original) {
        BlockPos ground = original.call(level, types, pos);
        if (types == Heightmap.Types.MOTION_BLOCKING) {
            BlockPos top = new BlockPos(ground.getX(), SweptSnow.surface(level, ground.getX(), ground.getZ()), ground.getZ());
            if (top.getY() > ground.getY() && level.getBlockState(top.below()).getBlock() instanceof LeavesBlock
                    && level.getBiome(top).value().shouldSnow(level, top)) {
                return top;
            }
        }
        return ground;
    }
}

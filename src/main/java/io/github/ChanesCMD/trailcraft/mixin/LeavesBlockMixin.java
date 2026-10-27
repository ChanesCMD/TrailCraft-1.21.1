package io.github.ChanesCMD.trailcraft.mixin;

import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * Любая листва (ванильная, своя, других модов) проходима: столкновений нет, внутри движение медленное, как в кустах,
 * а падение в листву гасится (makeStuckInBlock сбрасывает высоту падения). Тонкие брёвна не в #minecraft:logs,
 * поэтому листва рядом с ними считается растущей у ствола сама (#trailcraft:thin_logs).
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block {
    /** Множитель скорости в листве: по горизонтали вдвое медленнее, падение — заметно медленнее. */
    @Unique
    private static final Vec3 trailcraft$SLOWDOWN = new Vec3(0.5, 0.35, 0.5);

    private LeavesBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, trailcraft$SLOWDOWN);
    }

    @Inject(method = "getOptionalDistanceAt", at = @At("HEAD"), cancellable = true)
    private static void trailcraft$thinLogs(BlockState state, CallbackInfoReturnable<OptionalInt> cir) {
        if (state.is(ModTags.THIN_LOG_BLOCKS)) {
            cir.setReturnValue(OptionalInt.of(0));
        }
    }
}

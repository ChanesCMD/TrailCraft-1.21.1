package io.github.ChanesCMD.trailcraft.mixin;

import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.ChanesCMD.trailcraft.pipe.ReinforcedPipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ключ по прямой укреплённой трубе — укреплённая стеклянная. */
@Mixin(value = FluidPipeBlock.class, remap = false)
public abstract class FluidPipeBlockMixin {
    @Redirect(method = "onWrenched", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trailcraft$glass(BlockEntry<?> entry) {
        return ReinforcedPipes.swap(this, entry).defaultBlockState();
    }
}

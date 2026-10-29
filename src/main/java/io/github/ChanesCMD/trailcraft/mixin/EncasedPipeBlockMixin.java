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

/** В корпусе: ключом снять корпус — укреплённая труба, «взять блок» — труба. */
@Mixin(value = EncasedPipeBlock.class, remap = false)
public abstract class EncasedPipeBlockMixin {
    @Redirect(method = "onWrenched", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState trailcraft$state(BlockEntry<?> entry) {
        return ReinforcedPipes.swap(this, entry).defaultBlockState();
    }

    @Redirect(method = "onWrenched", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;get()Ljava/lang/Object;"))
    private Object trailcraft$block(BlockEntry<?> entry) {
        return ReinforcedPipes.swap(this, entry);
    }

    @Redirect(method = "getCloneItemStack", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack trailcraft$clone(BlockEntry<?> entry) {
        return new ItemStack(ReinforcedPipes.swap(this, entry));
    }
}

package io.github.ChanesCMD.trailcraft.mixin;

import com.simibubi.create.content.equipment.TreeFertilizerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Удобрение для деревьев Create ставит выращенное дерево поверх мира через destroyBlock(pos, true): саженец (и все
 * четыре у дерева 2×2, и соседние саженцы) выпадал предметом. Саженец и то, что дерево и так заменяет (трава, цветы),
 * теперь просто исчезают, как при костной муке.
 */
@Mixin(value = TreeFertilizerItem.class, remap = false)
public abstract class TreeFertilizerItemMixin {
    @Redirect(method = "useOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean trailcraft$noSaplingDrop(Level level, BlockPos pos, boolean drop) {
        BlockState old = level.getBlockState(pos);
        return level.destroyBlock(pos, drop && !old.is(BlockTags.SAPLINGS) && !old.is(BlockTags.REPLACEABLE_BY_TREES));
    }
}

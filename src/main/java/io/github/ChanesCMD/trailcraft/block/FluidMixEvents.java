package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Лава встречается с водой: вместо ванильного булыжника получается серый гранит (обсидиан остаётся). */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class FluidMixEvents {
    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getNewState().is(Blocks.COBBLESTONE)) {
            event.setNewState(ModBlocks.GRAY_GRANITE.get().defaultBlockState());
        }
    }

    private FluidMixEvents() {}
}

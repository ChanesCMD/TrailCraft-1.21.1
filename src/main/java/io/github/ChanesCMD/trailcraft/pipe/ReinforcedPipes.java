package io.github.ChanesCMD.trailcraft.pipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.tterrag.registrate.util.entry.BlockEntry;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Укреплённые трубы превращаются друг в друга, как трубы Create: Create берёт обычные трубы из AllBlocks, а
 * mixin/ReinforcedPipeMixins подменяет их укреплёнными, если превращается укреплённая труба.
 */
public final class ReinforcedPipes {
    private ReinforcedPipes() {}

    public static boolean isReinforcedPipe(Object block) {
        return block instanceof ReinforcedPipeBlock || block instanceof ReinforcedGlassPipeBlock
                || block instanceof ReinforcedEncasedPipeBlock;
    }

    /** Труба Create, в которую превращается {@code self}, — или её укреплённая версия. */
    public static Block swap(Object self, BlockEntry<?> entry) {
        Block block = entry.get();
        if (!isReinforcedPipe(self)) {
            return block;
        }
        if (block == AllBlocks.FLUID_PIPE.get()) return ModBlocks.REINFORCED_FLUID_PIPE.get();
        if (block == AllBlocks.GLASS_FLUID_PIPE.get()) return ModBlocks.REINFORCED_GLASS_FLUID_PIPE.get();
        if (block == AllBlocks.ENCASED_FLUID_PIPE.get()) return ModBlocks.REINFORCED_ENCASED_FLUID_PIPE.get();
        return block;
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        /** Медный корпус на укреплённую трубу — укреплённая труба в корпусе. */
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> EncasingRegistry.addVariant(ModBlocks.REINFORCED_FLUID_PIPE.get(),
                    ModBlocks.REINFORCED_ENCASED_FLUID_PIPE.get()));
        }
    }
}

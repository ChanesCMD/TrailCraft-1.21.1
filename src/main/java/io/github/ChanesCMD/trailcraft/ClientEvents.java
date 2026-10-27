package io.github.ChanesCMD.trailcraft;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import io.github.ChanesCMD.trailcraft.client.ThrownSpearRenderer;
import io.github.ChanesCMD.trailcraft.client.animal.CapercaillieModel;
import io.github.ChanesCMD.trailcraft.client.animal.CapercaillieRenderer;
import io.github.ChanesCMD.trailcraft.init.ModEntities;

@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    /** true = цвет травы берётся из биома (grass_color в JSON биома); false = один фиксированный цвет GRASS_COLOR везде. */
    private static final boolean USE_BIOME_TINT = true;

    /** Запасной цвет (для предметов в руке и при USE_BIOME_TINT = false): ванильная трава равнин #91BD59. */
    private static final int GRASS_COLOR = 0x91BD59;

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(), ThrownSpearRenderer::new);
        e.registerEntityRenderer(ModEntities.CAPERCAILLIE.get(), CapercaillieRenderer::new);
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CapercaillieModel.LAYER, CapercaillieModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, tint) ->
                        USE_BIOME_TINT && level != null && pos != null
                                ? BiomeColors.getAverageGrassColor(level, pos)
                                : GRASS_COLOR,
                // дёрн
                ModBlocks.BUROZEM_GRASS.get(),
                ModBlocks.CHERNOZEM_GRASS.get(),
                ModBlocks.ZHELTOZEM_GRASS.get(),
                ModBlocks.PEAT_GRASS.get(),
                ModBlocks.LOAM_GRASS.get(),
                ModBlocks.CLAY_BLUE_GRASS.get(),
                ModBlocks.CLAY_GRAY_GRASS.get(),
                ModBlocks.CLAY_WHITE_GRASS.get(),
                ModBlocks.CLAY_RED_GRASS.get(),
                // трава-растения
                ModBlocks.GRASS_SMALL.get(),
                ModBlocks.GRASS_MEDIUM.get(),
                ModBlocks.GRASS_TALL.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> GRASS_COLOR,
                ModItems.BUROZEM_GRASS.get(),
                ModItems.CHERNOZEM_GRASS.get(),
                ModItems.ZHELTOZEM_GRASS.get(),
                ModItems.PEAT_GRASS.get(),
                ModItems.LOAM_GRASS.get(),
                ModItems.CLAY_BLUE_GRASS.get(),
                ModItems.CLAY_GRAY_GRASS.get(),
                ModItems.CLAY_WHITE_GRASS.get(),
                ModItems.CLAY_RED_GRASS.get(),
                ModItems.GRASS_SMALL.get(),
                ModItems.GRASS_MEDIUM.get(),
                ModItems.GRASS_TALL.get());
    }
}

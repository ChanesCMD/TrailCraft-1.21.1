package io.github.ChanesCMD.trailcraft;

import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import io.github.ChanesCMD.trailcraft.client.BackpackLayer;
import io.github.ChanesCMD.trailcraft.client.ThrownSpearRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import io.github.ChanesCMD.trailcraft.client.TrailCampfireRenderer;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.client.animal.CapercaillieModel;
import io.github.ChanesCMD.trailcraft.client.animal.CapercaillieRenderer;
import io.github.ChanesCMD.trailcraft.client.animal.TrailAnimalModel;
import io.github.ChanesCMD.trailcraft.client.animal.TrailAnimalRenderer;
import io.github.ChanesCMD.trailcraft.animal.Species;
import io.github.ChanesCMD.trailcraft.init.ModAnimals;
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
        e.registerBlockEntityRenderer(ModBlockEntities.CAMPFIRE.get(), TrailCampfireRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.WOOD_CHEST.get(), io.github.ChanesCMD.trailcraft.client.WoodChestRenderer::new);
        for (Species species : Species.values()) {
            e.registerEntityRenderer(ModAnimals.type(species), context -> new TrailAnimalRenderer<>(context, species));
        }
    }

    /** Сундуки своих пород в руке и в инвентаре рисуются моделью сундука (client/WoodChestItemRenderer). */
    @SubscribeEvent
    public static void clientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) {
        io.github.ChanesCMD.trailcraft.client.WoodChestItemRenderer.register(e);
    }

    /** Рюкзак на спине — у обоих видов скина игрока. */
    @SubscribeEvent
    public static void playerLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new BackpackLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CapercaillieModel.LAYER, CapercaillieModel::createBodyLayer);
        for (Species species : Species.values()) {
            e.registerLayerDefinition(TrailAnimalRenderer.layer(species), () -> TrailAnimalModel.createLayer(TrailAnimalModel.load(species.id)));
        }
    }

    /** Укреплённая труба рисуется, как труба Create: ободки, стыки и сливы добавляет PipeAttachmentModel. */
    @SubscribeEvent
    public static void wrapPipeModels(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult e) {
        for (var state : ModBlocks.REINFORCED_FLUID_PIPE.get().getStateDefinition().getPossibleStates()) {
            e.getModels().computeIfPresent(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state),
                    (location, model) -> com.simibubi.create.content.fluids.PipeAttachmentModel.withAO(model));
        }
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

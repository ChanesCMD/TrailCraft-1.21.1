package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.entity.NuggetMarkerBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModOres;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * ВРЕМЕННО: над каждым самородком — столб из блока его руды высотой HEIGHT блоков, светящийся и видный издалека,
 * чтобы проверить генерацию (см. NuggetBlock).
 */
public class NuggetMarkerRenderer implements BlockEntityRenderer<NuggetMarkerBlockEntity> {
    private static final float HEIGHT = 40;
    private final BlockRenderDispatcher blocks;

    public NuggetMarkerRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(NuggetMarkerBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Ore ore = oreOf(entity.getBlockState().getBlock());
        if (ore == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.3, 0.15, 0.3);
        pose.scale(0.4f, HEIGHT, 0.4f);
        blocks.renderSingleBlock(ModOres.BLOCKS.get(ore).get().defaultBlockState(), pose, buffers,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static Ore oreOf(Block block) {
        for (var entry : ModOres.NUGGET_BLOCKS.entrySet()) {
            if (entry.getValue().get() == block) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean shouldRenderOffScreen(NuggetMarkerBlockEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(NuggetMarkerBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0, HEIGHT, 0);
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.NUGGET_MARKER.get(), NuggetMarkerRenderer::new);
        }
    }
}

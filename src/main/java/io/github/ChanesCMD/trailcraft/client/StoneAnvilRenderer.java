package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ChanesCMD.trailcraft.block.StoneAnvilBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Крица лежит на наковальне плашмя и не крутится. */
public class StoneAnvilRenderer implements BlockEntityRenderer<StoneAnvilBlockEntity> {
    private final ItemRenderer items;

    public StoneAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(StoneAnvilBlockEntity anvil, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        ItemStack bloom = anvil.getBloom();
        if (bloom.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.90, 0.5);
        pose.scale(0.55f, 0.55f, 0.55f);
        items.renderStatic(bloom, ItemDisplayContext.GROUND, packedLight, packedOverlay, pose, buffer,
                anvil.getLevel(), 0);
        pose.popPose();
    }
}

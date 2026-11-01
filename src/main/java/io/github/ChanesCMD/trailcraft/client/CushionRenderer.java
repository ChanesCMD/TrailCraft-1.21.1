package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.cushion.CushionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Подушка рисуется моделью своего предмета (models/item/&lt;цвет&gt;_cushion.json, текстура — шерсть нужного цвета). */
public class CushionRenderer extends EntityRenderer<CushionEntity> {
    private final ItemRenderer itemRenderer;

    public CushionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CushionEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.translate(0.0, 0.5, 0.0);  // модель предмета рисуется вокруг центра блока, а подушка стоит основанием в точке
        itemRenderer.renderStatic(entity.getItemStack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffer, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CushionEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

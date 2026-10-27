package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.spear.ThrownSpear;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Летящее и лежащее метательное копьё: иконка предмета, развёрнутая остриём по направлению полёта (как стрела:
 * ось +X — вперёд). На иконке остриё смотрит вверх-вправо, а вид FIXED ещё разворачивает её на 180° вокруг Y,
 * поэтому поворот по Z на −135°.
 */
public class ThrownSpearRenderer extends EntityRenderer<ThrownSpear> {
    private static final float SCALE = 1.6f;
    private final ItemRenderer items;

    public ThrownSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpear spear, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, spear.yRotO, spear.getYRot()) - 90.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, spear.xRotO, spear.getXRot()) - 135.0f));
        pose.scale(SCALE, SCALE, SCALE);
        items.renderStatic(spear.getItem(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                spear.level(), spear.getId());
        pose.popPose();
        super.render(spear, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownSpear spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

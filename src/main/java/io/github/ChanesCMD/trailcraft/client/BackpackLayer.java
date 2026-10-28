package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Надетый рюкзак на спине игрока: модель его блока (как поставленный на землю рюкзак, scripts/gen_bbmodels.py),
 * карманом наружу. Какой рюкзак надет, присылает сервер вместе с косметикой (ClientCosmetics.backpack).
 * Модель блока ставится на спину так: середина её задней стенки (x 8, z BACK) — к спине игрока, низ — на BOTTOM
 * пикселей ниже плеч.
 */
public class BackpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Задняя стенка рюкзаков в модели блока (z, пиксели); где низ рюкзака на спине и насколько он отстоит от тела. */
    private static final float BACK = 5 / 16f, BOTTOM = 10 / 16f, FROM_BODY = 2.5f / 16f;

    public BackpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        ItemStack backpack = ClientCosmetics.backpack(player.getId());
        Block block = backpack.isEmpty() ? Blocks.AIR : Block.byItem(backpack.getItem());
        if (block == Blocks.AIR) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // в модели игрока y смотрит вниз, спина — в +z; блок переворачиваем вокруг z, чтобы верх был вверху
        pose.translate(0.0f, BOTTOM, FROM_BODY);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        pose.translate(-0.5f, 0.0f, -BACK);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), pose, buffers, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}

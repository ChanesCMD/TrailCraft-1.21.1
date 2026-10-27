package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.item.ClothBlankItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Обработка тканевой заготовки от первого лица — как шлифовка розового кварца наждачкой в Create: заготовку держат
 * перед лицом, как еду (только она не подпрыгивает), а палка из другой руки лежит поперёк неё и трёт её вверх-вниз.
 * Обе вещи рисует рука с заготовкой, руку с палкой не рисуем.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientClothEvents {
    /** Ход палки по заготовке (доля размера предмета) и период одного движения туда-обратно, тики. */
    private static final float RUB_STROKE = 0.2f;
    private static final float RUB_PERIOD = 8;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof ClothBlankItem)) {
            return;
        }
        InteractionHand blankHand = player.getUsedItemHand();
        InteractionHand stickHand = blankHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stick = player.getItemInHand(stickHand);
        if (!stick.is(ModItems.STICK_PROCESSED.get())) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == blankHand) {
            render(player, event.getItemStack(), stick, blankHand, event.getPoseStack(), event.getMultiBufferSource(),
                    event.getPackedLight(), event.getPartialTick(), event.getEquipProgress());
        }
    }

    private static void render(LocalPlayer player, ItemStack blank, ItemStack stick, InteractionHand hand, PoseStack poseStack,
                               MultiBufferSource buffer, int light, float partialTick, float equipProgress) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float time = player.getUseItemRemainingTicks() - partialTick + 1;
        float progress = time / blank.getUseDuration(player);
        // Как ItemInHandRenderer.applyEatTransform, но без подпрыгивания: подпрыгивает палка
        float raise = 1 - (float) Math.pow(progress, 27);

        poseStack.pushPose();
        poseStack.translate(raise * 0.6f * side, raise * -0.5f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * raise * 90));
        poseStack.mulPose(Axis.XP.rotationDegrees(raise * 10));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * raise * 30));
        poseStack.translate(side * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);

        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        ItemDisplayContext context = side > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        BakedModel model = items.getModel(blank, player.level(), player, player.getId() + context.ordinal());
        // Дальше — в координатах самой заготовки: центр в нуле, размер 1, к камере смотрит +Z
        model = ClientHooks.handleCameraTransforms(poseStack, model, context, side < 0);
        items.render(blank, ItemDisplayContext.NONE, false, poseStack, buffer, light, OverlayTexture.NO_OVERLAY, model);

        // Палка лежит поперёк заготовки перед ней и ходит вверх-вниз; крайние точки — на звуке трения (раз в 4 тика)
        float rub = Mth.cos(time / RUB_PERIOD * Mth.TWO_PI) * RUB_STROKE;
        poseStack.translate(side * 0.05f, 0.05f + rub, 0.08f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (-30 + 8 * Mth.sin(time / RUB_PERIOD * Mth.TWO_PI))));
        poseStack.scale(0.9f, 0.9f, 0.9f);
        items.renderStatic(player, stick, ItemDisplayContext.NONE, side < 0, poseStack, buffer, player.level(), light,
                OverlayTexture.NO_OVERLAY, player.getId() + ItemDisplayContext.NONE.ordinal());
        poseStack.popPose();
    }
}

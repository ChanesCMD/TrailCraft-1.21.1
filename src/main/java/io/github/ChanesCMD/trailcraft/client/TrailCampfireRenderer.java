package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.TrailCampfireBlock;
import io.github.ChanesCMD.trailcraft.block.entity.TrailCampfireBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Еда на костре TrailCraft: лежит плашмя, как на ванильном костре, но на высоте своего бревна (или на золе —
 * TrailCampfireBlock.slotPosition), а под ней — плоская глиняная тарелочка (textures/block/campfire_plate.png:
 * тарелка 6×6 в середине текстуры 16×16).
 */
public class TrailCampfireRenderer implements BlockEntityRenderer<TrailCampfireBlockEntity> {
    private static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "block/campfire_plate");
    /** Тарелка — 6 пикселей, предмет — как у ванильного костра (тоже 6 пикселей), чуть выше тарелки. */
    private static final float PLATE_HALF = 3 / 16f, ITEM_SCALE = 0.375f, PLATE_LIFT = 0.002f, ITEM_LIFT = 0.016f;
    /** Где тарелка в текстуре: пиксели 5..11 из 16. */
    private static final float PLATE_UV0 = 0.5f - PLATE_HALF, PLATE_UV1 = 0.5f + PLATE_HALF;

    private final ItemRenderer itemRenderer;

    public TrailCampfireRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TrailCampfireBlockEntity campfire, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = campfire.getBlockState();
        float yaw = -state.getValue(TrailCampfireBlock.FACING).toYRot();
        TextureAtlasSprite plate = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(PLATE);
        VertexConsumer plates = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        int seed = (int) campfire.getBlockPos().asLong();
        for (int i = 0; i < TrailCampfireBlockEntity.SLOTS; i++) {
            ItemStack stack = campfire.getItems().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            Vec3 at = TrailCampfireBlock.slotPosition(state, i);
            plate(pose, plates, plate, (float) at.x, (float) at.y + PLATE_LIFT, (float) at.z, light, overlay);
            pose.pushPose();
            pose.translate(at.x, at.y + ITEM_LIFT, at.z);
            pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f * i));
            pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, campfire.getLevel(), seed + i);
            pose.popPose();
        }
    }

    /** Плоский квадрат тарелки лицом вверх; берётся середина текстуры (PLATE_UV0..PLATE_UV1). */
    private static void plate(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, float x, float y, float z,
                              int light, int overlay) {
        float u0 = lerp(sprite.getU0(), sprite.getU1(), PLATE_UV0), u1 = lerp(sprite.getU0(), sprite.getU1(), PLATE_UV1);
        float v0 = lerp(sprite.getV0(), sprite.getV1(), PLATE_UV0), v1 = lerp(sprite.getV0(), sprite.getV1(), PLATE_UV1);
        PoseStack.Pose last = pose.last();
        vertex(out, last, x - PLATE_HALF, y, z - PLATE_HALF, u0, v0, light, overlay);
        vertex(out, last, x - PLATE_HALF, y, z + PLATE_HALF, u0, v1, light, overlay);
        vertex(out, last, x + PLATE_HALF, y, z + PLATE_HALF, u1, v1, light, overlay);
        vertex(out, last, x + PLATE_HALF, y, z - PLATE_HALF, u1, v0, light, overlay);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int light, int overlay) {
        out.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

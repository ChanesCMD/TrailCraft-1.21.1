package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.item.BroomItem;
import io.github.ChanesCMD.trailcraft.season.SweptSnowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * Метла в руке подсвечивает места, куда снег не падает (сметённые столбцы, season/SweptSnow): голубая плашка с рамкой
 * поверх верхнего блока. Список приходит от сервера (season/BroomEvents) раз в полсекунды, пока метла в руке.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class SweptSnowOverlay {
    private static volatile List<BlockPos> columns = List.of();

    public static void receive(SweptSnowPayload payload) {
        columns = payload.columns();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        List<BlockPos> shown = columns;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (shown.isEmpty() || player == null
                || !(player.getMainHandItem().getItem() instanceof BroomItem || player.getOffhandItem().getItem() instanceof BroomItem)) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        for (BlockPos c : shown) {
            LevelRenderer.addChainedFilledBoxVertices(pose, fill,
                    c.getX() + 0.03, c.getY() + 0.005, c.getZ() + 0.03, c.getX() + 0.97, c.getY() + 0.03, c.getZ() + 0.97,
                    0.35f, 0.75f, 1.0f, 0.40f);
        }
        buffers.endBatch(RenderType.debugFilledBox());

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos c : shown) {
            LevelRenderer.renderLineBox(pose, lines,
                    c.getX() + 0.03, c.getY() + 0.005, c.getZ() + 0.03, c.getX() + 0.97, c.getY() + 0.03, c.getZ() + 0.97,
                    0.55f, 0.9f, 1.0f, 0.9f);
        }
        buffers.endBatch(RenderType.lines());

        pose.popPose();
    }

    private SweptSnowOverlay() {}
}

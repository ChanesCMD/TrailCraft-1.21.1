package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.block.LogPileBlock;
import io.github.ChanesCMD.trailcraft.block.ThinLogBlock;
import io.github.ChanesCMD.trailcraft.block.entity.LogPileBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Брёвна поленницы лежат горизонтально. Слот — клетка 4×4 пикселя в сечении (LogPileBlockEntity.slotAt), сетка
 * занимает весь блок: слева направо, потом снизу вверх. Толстое бревно — одно в клетку, тонких в клетке до четырёх, по
 * 2×2 (на 4 тонких в слоте приходится одно нарисованное, слот из 16 — все четыре). Тонкое бревно — труба с
 * ответвлениями (ThinLogBlock), поэтому его состояние собирается вручную: ответвления только по оси брёвен.
 */
public class LogPileRenderer implements BlockEntityRenderer<LogPileBlockEntity> {
    private static final float CELL = LogPileBlockEntity.CELL, MARGIN = LogPileBlockEntity.MARGIN;
    /** Тонкое бревно занимает 6 из 16 пикселей блока, а в клетке для него — половина клетки. */
    private static final float THIN_SCALE = (CELL / 2) / (6 / 16f);
    private static final int THIN_PER_CELL = 4;

    private final BlockRenderDispatcher blocks;

    public LogPileRenderer(BlockEntityRendererProvider.Context context) {
        this.blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LogPileBlockEntity pile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        if (pile.getBlockState().getValue(LogPileBlock.AXIS) == Direction.Axis.X) {
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(-0.5, 0, -0.5);
        }
        for (int slot = 0; slot < LogPileBlockEntity.SLOTS; slot++) {
            ItemStack stack = pile.getItems().get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
                continue;
            }
            float x = MARGIN + slot % LogPileBlockEntity.COLUMNS * CELL, y = slot / LogPileBlockEntity.COLUMNS * CELL;
            if (stack.is(ModTags.THIN_LOGS)) {
                BlockState thin = thinAlongZ(item.getBlock().defaultBlockState());
                int shown = Math.max(1, (int) Math.ceil(stack.getCount() * (double) THIN_PER_CELL / LogPileBlockEntity.THIN_CAPACITY));
                for (int i = 0; i < shown; i++) {
                    float half = CELL / 2;
                    pose.pushPose();
                    pose.translate(x + i % 2 * half + half / 2, y + i / 2 * half + half / 2, 0);
                    pose.scale(THIN_SCALE, THIN_SCALE, 1f);
                    pose.translate(-0.5, -0.5, 0);
                    blocks.renderSingleBlock(thin, pose, buffers, light, overlay);
                    pose.popPose();
                }
            } else {
                BlockState log = item.getBlock().defaultBlockState();
                if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                    log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                }
                pose.pushPose();
                pose.translate(x, y, 0);
                pose.scale(CELL, CELL, 1f);
                blocks.renderSingleBlock(log, pose, buffers, light, overlay);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    /** Тонкое бревно во весь блок вдоль Z: ответвления только на север и юг. */
    private static BlockState thinAlongZ(BlockState state) {
        if (!(state.getBlock() instanceof ThinLogBlock)) {
            return state;
        }
        for (var property : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        return state.setValue(PipeBlock.NORTH, true).setValue(PipeBlock.SOUTH, true);
    }
}

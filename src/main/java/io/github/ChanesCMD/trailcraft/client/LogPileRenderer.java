package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.block.LogPileBlock;
import io.github.ChanesCMD.trailcraft.block.ThinLogBlock;
import io.github.ChanesCMD.trailcraft.block.entity.LogPileBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Брёвна поленницы лежат горизонтально. Слот — клетка 4×4 пикселя в сечении (LogPileBlockEntity.slotAt), сетка
 * занимает весь блок: слева направо, потом снизу вверх. Толстое бревно — одно в клетку, тонких в клетке до четырёх, по
 * 2×2 (на 4 тонких в слоте приходится одно нарисованное, слот из 16 — все четыре). Тонкое бревно — труба с
 * ответвлениями (ThinLogBlock), поэтому его состояние собирается вручную: ответвления только по оси брёвен.
 */
public class LogPileRenderer implements BlockEntityRenderer<LogPileBlockEntity> {
    private static final float CELL = LogPileBlockEntity.CELL, MARGIN = LogPileBlockEntity.MARGIN;
    /** Подпорки: столбик 2×2 пикселя на каждом краю, вдоль брёвен — на четверти и на трёх четвертях длины. */
    private static final float STAKE = 2 / 16f;
    private static final float[] STAKE_Z = {4 / 16f, 12 / 16f};
    /** Текстура ровной обработанной палки: два средних текселя рисунка. */
    private static final ResourceLocation STAKE_TEXTURE = ResourceLocation.fromNamespaceAndPath("trailcraft", "block/stake");
    /** Тонкое бревно занимает 6 из 16 пикселей блока, а в клетке для него — половина клетки. */
    private static final float THIN_SCALE = (CELL / 2) / (6 / 16f);
    private static final int THIN_PER_CELL = 4;
    /** Бок бревна собирается из четырёх уменьшенных брёвен подряд: так текстура ложится 64x16 на грань 16x4. */
    private static final int SEGMENTS = 4;

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
                    for (int seg = 0; seg < SEGMENTS; seg++) {
                        pose.pushPose();
                        pose.translate(x + i % 2 * half + half / 2, y + i / 2 * half + half / 2, seg / (float) SEGMENTS);
                        pose.scale(THIN_SCALE, THIN_SCALE, 1f / SEGMENTS);
                        pose.translate(-0.5, -0.5, 0);
                        blocks.renderSingleBlock(thin, pose, buffers, light, overlay);
                        pose.popPose();
                    }
                }
            } else {
                BlockState log = item.getBlock().defaultBlockState();
                if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                    log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                }
                // бок — четыре уменьшенных бревна подряд: текстура 64x16 ложится на грань 16x4
                for (int seg = 0; seg < SEGMENTS; seg++) {
                    pose.pushPose();
                    pose.translate(x, y, seg / (float) SEGMENTS);
                    pose.scale(CELL, CELL, 1f / SEGMENTS);
                    blocks.renderSingleBlock(log, pose, buffers, light, overlay);
                    pose.popPose();
                }
            }
        }
        renderStakes(pile, pose, buffers, light, overlay);
        pose.popPose();
    }

    /**
     * Четыре палки по краям: 16 пикселей в высоту, сдвинуты на пиксель вниз и на пиксель в стороны от поленницы.
     * Рисуются коробкой 2×2 пикселя с текстурой ровной обработанной палки.
     */
    private void renderStakes(LogPileBlockEntity pile, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (pile.topRow() < 0) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(STAKE_TEXTURE);
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        for (float edge : new float[]{STAKE / 2 - 1 / 16f, 1 - STAKE / 2 + 1 / 16f}) {
            for (float z : STAKE_Z) {
                stake(pose, vc, edge, z, sprite, light, overlay);
            }
        }
    }

    /** Палка: коробка 2×2×16 пикселей, бока — 2 текселя ширины на 16 высоты, торцы — 2×2. */
    private static void stake(PoseStack pose, VertexConsumer vc, float cx, float cz, TextureAtlasSprite sprite,
                              int light, int overlay) {
        float half = STAKE / 2;
        float x0 = cx - half, x1 = cx + half, z0 = cz - half, z1 = cz + half, y0 = -1 / 16f, y1 = 15 / 16f;
        float texU = (sprite.getU1() - sprite.getU0()) / 16f, texV = (sprite.getV1() - sprite.getV0()) / 16f;
        float u0 = sprite.getU0() + 7 * texU, du = 2 * texU, v0 = sprite.getV0(), dv = 16 * texV, de = 2 * texV;
        PoseStack.Pose p = pose.last();
        face(vc, p, new float[]{x0, y0, z0}, new float[]{x0, y0, z1}, new float[]{x0, y1, z1}, new float[]{x0, y1, z0},
                u0, v0, du, dv, light, overlay, 0.65f, -1, 0, 0);
        face(vc, p, new float[]{x1, y0, z1}, new float[]{x1, y0, z0}, new float[]{x1, y1, z0}, new float[]{x1, y1, z1},
                u0, v0, du, dv, light, overlay, 0.65f, 1, 0, 0);
        face(vc, p, new float[]{x0, y0, z1}, new float[]{x1, y0, z1}, new float[]{x1, y1, z1}, new float[]{x0, y1, z1},
                u0, v0, du, dv, light, overlay, 0.8f, 0, 0, 1);
        face(vc, p, new float[]{x1, y0, z0}, new float[]{x0, y0, z0}, new float[]{x0, y1, z0}, new float[]{x1, y1, z0},
                u0, v0, du, dv, light, overlay, 0.8f, 0, 0, -1);
        face(vc, p, new float[]{x0, y1, z0}, new float[]{x1, y1, z0}, new float[]{x1, y1, z1}, new float[]{x0, y1, z1},
                u0, v0, du, de, light, overlay, 1f, 0, 1, 0);
        face(vc, p, new float[]{x0, y0, z1}, new float[]{x1, y0, z1}, new float[]{x1, y0, z0}, new float[]{x0, y0, z0},
                u0, v0, du, de, light, overlay, 0.5f, 0, -1, 0);
    }

    /** Грань по четырём углам с окном текстуры u0/v0 размера du×dv, с двух сторон (от намотки не зависим). */
    private static void face(VertexConsumer vc, PoseStack.Pose pose, float[] a, float[] b, float[] c, float[] d,
                             float u0, float v0, float du, float dv, int light, int overlay, float shade,
                             float nx, float ny, float nz) {
        VertexConsumer out = vc;
        float[][] corners = {a, b, c, d};
        float[][] uv = {{0, 0}, {du, 0}, {du, dv}, {0, dv}};
        Vector3f pos = new Vector3f();
        Vector3f normal = pose.transformNormal(nx, ny, nz, new Vector3f());
        int color = (int) (shade * 255);
        color = color << 16 | color << 8 | color | 0xFF000000;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                int k = pass == 0 ? i : 3 - i;
                pos.set(corners[k][0], corners[k][1], corners[k][2]);
                pose.pose().transformPosition(pos);
                float u = u0 + (pass == 0 ? uv[k][0] : -uv[k][0] + du);
                float v = v0 + (pass == 0 ? uv[k][1] : -uv[k][1] + dv);
                out.addVertex(pos.x, pos.y, pos.z)
                        .setColor(color)
                        .setUv(u, v)
                        .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                        .setOverlay(overlay)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
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

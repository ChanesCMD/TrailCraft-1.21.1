package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.ChanesCMD.trailcraft.block.LogPileBlock;
import io.github.ChanesCMD.trailcraft.block.ThinLogBlock;
import io.github.ChanesCMD.trailcraft.block.entity.LogPileBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Брёвна поленницы лежат горизонтально. Слот — клетка {@code CELL} (4×4 пикселя) в сечении: слева направо, потом
 * снизу вверх. Брёвна лежат вплотную к краю блока, между ними стоят палки-подпорки — тонкие (1 пиксель) столбики в два
 * блока высотой: один блок торчит над поленницей, один внутри неё. Толстое бревно — одно в клетку, тонких в клетке до
 * четырёх, по 2×2. Текстура бревна рисуется 1:1 (не растягивается на клетку): торцы — кусок текстуры торца, бока —
 * полоса коры во всю длину бревна. Тонкое бревно — труба с ответвлениями (ThinLogBlock), её состояние собирается
 * вручную: ответвления только по оси брёвен.
 */
public class LogPileRenderer implements BlockEntityRenderer<LogPileBlockEntity> {
    private static final float CELL = LogPileBlockEntity.CELL, MARGIN = LogPileBlockEntity.MARGIN;
    /** Подпорки: палка 1 пиксель толщиной и в один блок высотой (текстура столбика не растягивается). */
    private static final float STAKE = 1 / 16f, STAKE_HEIGHT = 1f;
    /** Палки стоят по краям (поперёк брёвен) с отступом от торцов на четверть длины. */
    private static final float[] STAKE_X = {0f, 1f};
    private static final float[] STAKE_Z = {4 / 16f, 12 / 16f};
    private static final BlockState STAKE_POST = Blocks.OAK_FENCE.defaultBlockState();
    private static final int THIN_PER_CELL = 4;
    private static final RandomSource RANDOM = RandomSource.create(42L);

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
                float half = CELL / 2;
                for (int i = 0; i < shown; i++) {
                    renderLog(thin, pose, buffers, light, overlay, x + i % 2 * half, y + i / 2 * half, half,
                            Direction.Axis.Z, false);
                }
            } else {
                BlockState log = item.getBlock().defaultBlockState();
                if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                    log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                }
                renderLog(log, pose, buffers, light, overlay, x, y, CELL, Direction.Axis.Z, true);
            }
        }
        renderStakes(pile, pose, buffers, light, overlay);
        pose.popPose();
    }

    /**
     * Бревно: коробка {@code size}×{@code size}×16 пикселей вдоль Z, текстура 1:1. Бока — кора во всю длину бревна
     * (волокна вдоль Z), торцы — текстура торца: у толстого бревна целиком (видны кольца), у тонкого — кусок 1:1.
     * Спрайты берутся по оси бревна (не по мировой оси Y), грани рисуются с двух сторон — от намотки вершин не зависим.
     */
    private void renderLog(BlockState log, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                           float x, float y, float size, Direction.Axis axis, boolean fullEnd) {
        BakedModel model = blocks.getBlockModel(log);
        TextureAtlasSprite side = null, end = null;
        for (Direction dir : Direction.values()) {
            java.util.List<BakedQuad> quads = model.getQuads(log, dir, RANDOM);
            if (quads.isEmpty()) {
                continue;
            }
            if (dir.getAxis() == axis) {
                if (end == null) {
                    end = quads.get(0).getSprite();
                }
            } else if (side == null) {
                side = quads.get(0).getSprite();
            }
        }
        if (side == null) {
            side = model.getParticleIcon();
        }
        if (end == null) {
            end = side;
        }
        float x0 = x, x1 = x + size, y0 = y, y1 = y + size, z0 = 0f, z1 = 1f;
        float cross = size * 16f, length = 16f, endPx = fullEnd ? 16f : cross;
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        PoseStack.Pose p = pose.last();
        // низ и верх — кора во всю длину
        box(vc, p, new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}}, side, cross, length,
                light, overlay, 0.5f, 0, -1, 0);
        box(vc, p, new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, side, cross, length,
                light, overlay, 1.0f, 0, 1, 0);
        // бока — кора: волокна вдоль бревна (U поперёк, V по длине)
        box(vc, p, new float[][]{{x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}}, side, cross, length,
                light, overlay, 0.6f, -1, 0, 0);
        box(vc, p, new float[][]{{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}}, side, cross, length,
                light, overlay, 0.6f, 1, 0, 0);
        // торцы — кусок текстуры торца
        box(vc, p, new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}}, end, endPx, endPx,
                light, overlay, 0.8f, 0, 0, -1);
        box(vc, p, new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}}, end, endPx, endPx,
                light, overlay, 0.8f, 0, 0, 1);
    }

    /** Грань по четырём углам: UV от левого верхнего угла спрайта, окно width×height пикселей; с обеих сторон. */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, float[][] corners, TextureAtlasSprite sprite,
                            float widthPx, float heightPx, int light, int overlay, float shade, float nx, float ny, float nz) {
        float texU = (sprite.getU1() - sprite.getU0()) / 16f, texV = (sprite.getV1() - sprite.getV0()) / 16f;
        uvQuad(vc, pose, corners, sprite.getU0(), sprite.getV0(), widthPx * texU, heightPx * texV,
                light, overlay, shade, nx, ny, nz);
        float[][] reversed = {corners[3], corners[2], corners[1], corners[0]};
        uvQuad(vc, pose, reversed, sprite.getU0(), sprite.getV0(), widthPx * texU, heightPx * texV,
                light, overlay, shade, -nx, -ny, -nz);
    }

    private static void uvQuad(VertexConsumer vc, PoseStack.Pose pose, float[][] corners, float u0, float v0,
                               float du, float dv, int light, int overlay, float shade, float nx, float ny, float nz) {
        float[][] uv = {{0, 0}, {du, 0}, {du, dv}, {0, dv}};
        Vector3f pos = new Vector3f();
        Vector3f normal = pose.transformNormal(nx, ny, nz, new Vector3f());
        int color = (int) (shade * 255) << 16 | (int) (shade * 255) << 8 | (int) (shade * 255);
        for (int i = 0; i < 4; i++) {
            pos.set(corners[i][0], corners[i][1], corners[i][2]);
            pose.pose().transformPosition(pos);
            vc.addVertex(pos.x, pos.y, pos.z)
                    .setColor(color | 0xFF000000)
                    .setUv(u0 + uv[i][0], v0 + uv[i][1])
                    .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                    .setOverlay(overlay)
                    .setNormal(normal.x, normal.y, normal.z);
        }
    }

    /** Палки-подпорки по краям: 1 пиксель толщиной, в два блока высотой (поленница — нижний блок). */
    private void renderStakes(LogPileBlockEntity pile, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int top = pile.topRow();
        if (top < 0) {
            return;
        }
        for (float edge : STAKE_X) {
            for (float z : STAKE_Z) {
                pose.pushPose();
                pose.translate(edge, 0, z);
                pose.scale(STAKE / (4 / 16f), STAKE_HEIGHT, STAKE / (4 / 16f));  // у столбика забора сечение 4×4 пикселя
                pose.translate(-0.5, 0, -0.5);
                blocks.renderSingleBlock(STAKE_POST, pose, buffers, light, overlay);
                pose.popPose();
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

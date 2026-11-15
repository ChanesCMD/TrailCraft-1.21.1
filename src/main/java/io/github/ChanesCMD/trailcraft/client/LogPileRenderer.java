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
    /** Подпорки: палка 1 пиксель толщиной и в один блок высотой без одного пикселя. */
    private static final float STAKE = 1 / 16f, STAKE_HEIGHT = 15 / 16f;
    /** Сколько пикселей текстуры приходится на торец и ширину бока: у толстого бревна 8, у тонкого 4. */
    private static final float THICK_TEXELS = 8f, THIN_TEXELS = 4f;
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
                            Direction.Axis.Z, THIN_TEXELS);
                }
            } else {
                BlockState log = item.getBlock().defaultBlockState();
                if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                    log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                }
                renderLog(log, pose, buffers, light, overlay, x, y, CELL, Direction.Axis.Z, THICK_TEXELS);
            }
        }
        renderStakes(pile, pose, buffers, light, overlay);
        pose.popPose();
    }

    /**
     * Бревно: коробка {@code size}×{@code size}×16 пикселей вдоль Z. Бока — кора: по ширине 8 пикселей текстуры у
     * толстого и 4 у тонкого, по длине — 32 (две половины по 16, текстура повторяется и не вылезает за спрайт). Торцы — вся
     * текстура торца с корой по краю, сжатая в размер бревна. Спрайты берутся по оси бревна (не по мировой оси Y), грани
     * рисуются с двух сторон — от намотки вершин не зависим.
     */
    private void renderLog(BlockState log, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                           float x, float y, float size, Direction.Axis axis, float texelWidth) {
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
        float x0 = x, x1 = x + size, y0 = y, y1 = y + size, lengthTexels = 16f;
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        PoseStack.Pose p = pose.last();
        // низ, верх и бока — кора: две половины вдоль бревна, в каждой своя копия текстуры (по длине — 32 пикселя)
        for (float z : new float[]{0f, 0.5f}) {
            float z2 = z + 0.5f;
            box(vc, p, new float[][]{{x0, y0, z}, {x1, y0, z}, {x1, y0, z2}, {x0, y0, z2}}, side, texelWidth, lengthTexels,
                    light, overlay, 0.5f, 0, -1, 0);
            box(vc, p, new float[][]{{x0, y1, z}, {x1, y1, z}, {x1, y1, z2}, {x0, y1, z2}}, side, texelWidth, lengthTexels,
                    light, overlay, 1.0f, 0, 1, 0);
            box(vc, p, new float[][]{{x0, y0, z}, {x0, y1, z}, {x0, y1, z2}, {x0, y0, z2}}, side, texelWidth, lengthTexels,
                    light, overlay, 0.6f, -1, 0, 0);
            box(vc, p, new float[][]{{x1, y0, z}, {x1, y1, z}, {x1, y1, z2}, {x1, y0, z2}}, side, texelWidth, lengthTexels,
                    light, overlay, 0.6f, 1, 0, 0);
        }
        // торцы — вся текстура торца, сжатая до 8 пикселей у толстого бревна и до 4 у тонкого: каждый пиксель
        // результата берёт середину своего блока оригинала, поэтому кора по краю не теряется
        endFace(vc, p, x0, y0, x1, y1, 0f, (int) texelWidth, end, light, overlay, -1);
        endFace(vc, p, x0, y0, x1, y1, 1f, (int) texelWidth, end, light, overlay, 1);
    }

    /** Торец бревна, сжатый до {@code n}×{@code n} пикселей: один квад на пиксель, цвет — из середины блока текстуры. */
    private void endFace(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z,
                         int n, TextureAtlasSprite sprite, int light, int overlay, int normal) {
        float stepX = (x1 - x0) / n, stepY = (y1 - y0) / n;
        float texU = (sprite.getU1() - sprite.getU0()) / 16f, texV = (sprite.getV1() - sprite.getV0()) / 16f;
        float block = 16f / n;
        for (int iy = 0; iy < n; iy++) {
            for (int ix = 0; ix < n; ix++) {
                // крайние пиксели берут кору у самой кромки спрайта, внутренние — середину своего блока
                float u = sprite.getU0() + texel(ix, n, block) * texU;
                float v = sprite.getV0() + texel(iy, n, block) * texV;
                float ax = x0 + ix * stepX, bx = ax + stepX;
                float ay = y0 + iy * stepY, by = ay + stepY;
                // у тёмных пород (эбен, сосна, туя, железное дерево) кора и сердцевина почти одного цвета —
                // множители считаются по самой текстуре и растягивают слабый контраст
                int ring = Math.min(Math.min(ix, n - 1 - ix), Math.min(iy, n - 1 - iy));
                float[] factors = contrast(sprite);
                float shade = ring == 0 ? factors[0] : ring == 1 ? (factors[0] + factors[1]) / 2f : factors[1];
                pixel(vc, p, ax, ay, bx, by, z, u, v, light, overlay, shade, normal);
            }
        }
    }


    private static final java.util.Map<TextureAtlasSprite, float[]> CONTRAST = new java.util.HashMap<>();

    /**
     * Множители яркости для коры и сердцевины торца: если у породы они почти одного цвета (эбен, сосна, туя,
     * железное дерево), разница растягивается до заметной. Считается один раз на спрайт по его картинке.
     */
    private static float[] contrast(TextureAtlasSprite sprite) {
        return CONTRAST.computeIfAbsent(sprite, s -> {
            float edge = 0, inner = 0, edgeN = 0, innerN = 0;
            try {
                net.minecraft.client.renderer.texture.SpriteContents contents = s.contents();
                var img = contents.getOriginalImage();
                int w = img.getWidth(), h = img.getHeight();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int c = img.getPixelRGBA(x, y);
                        if (((c >>> 24) & 0xFF) < 128) {
                            continue;
                        }
                        float lum = 0.299f * (c & 0xFF) + 0.587f * ((c >> 8) & 0xFF) + 0.114f * ((c >> 16) & 0xFF);
                        boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                        if (border) {
                            edge += lum;
                            edgeN++;
                        } else {
                            inner += lum;
                            innerN++;
                        }
                    }
                }
            } catch (RuntimeException e) {
                return new float[]{1f, 1f};
            }
            if (edgeN == 0 || innerN == 0) {
                return new float[]{1f, 1f};
            }
            float e = edge / edgeN, i = inner / innerN, diff = e - i;
            float target = 90f;
            if (Math.abs(diff) >= target) {
                return new float[]{1f, 1f};
            }
            float sign = diff >= 0 ? 1f : -1f, extra = (target - Math.abs(diff)) / 2f;
            float fe = e <= 1f ? 1f : Math.max(0.25f, Math.min(2.2f, (e + sign * extra) / e));
            float fi = i <= 1f ? 1f : Math.max(0.25f, Math.min(2.2f, (i - sign * extra) / i));
            return new float[]{fe, fi};
        });
    }

    /** Номер текселя текстуры для пикселя торца: у кромки — самый край (там кора), внутри — середина блока. */
    private static float texel(int i, int n, float block) {
        if (i == 0) {
            return 0.5f;
        }
        if (i == n - 1) {
            return 15.5f;
        }
        return i * block + block / 2f;
    }

    /** Один пиксель торца: квад с постоянным UV (цвет берётся из одной точки текстуры), с двух сторон. */
    private static void pixel(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z,
                              float u, float v, int light, int overlay, float shade, int normal) {
        int color = (int) (shade * 255);
        color = color << 16 | color << 8 | color | 0xFF000000;
        Vector3f pos = new Vector3f();
        Vector3f n = p.transformNormal(0, 0, normal, new Vector3f());
        float[][] corners = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
        for (int i = 0; i < 8; i++) {
            float[] c = corners[i < 4 ? i : 7 - i];
            pos.set(c[0], c[1], z);
            p.pose().transformPosition(pos);
            vc.addVertex(pos.x, pos.y, pos.z)
                    .setColor(color)
                    .setUv(u, v)
                    .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                    .setOverlay(overlay)
                    .setNormal(n.x, n.y, n.z);
        }
    }

    /**
     * Грань по четырём углам: UV — окно width×height пикселей по центру спрайта (у торца бревна кольца как раз в
     * середине, у коры — середина полосы). Рисуется с обеих сторон.
     */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, float[][] corners, TextureAtlasSprite sprite,
                            float widthPx, float heightPx, int light, int overlay, float shade, float nx, float ny, float nz) {
        float texU = (sprite.getU1() - sprite.getU0()) / 16f, texV = (sprite.getV1() - sprite.getV0()) / 16f;
        float u0 = sprite.getU0() + (16f - widthPx) / 2f * texU;
        float v0 = sprite.getV0() + (16f - heightPx) / 2f * texV;
        uvQuad(vc, pose, corners, u0, v0, widthPx * texU, heightPx * texV,
                light, overlay, shade, nx, ny, nz);
        float[][] reversed = {corners[3], corners[2], corners[1], corners[0]};
        uvQuad(vc, pose, reversed, u0, v0, widthPx * texU, heightPx * texV,
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

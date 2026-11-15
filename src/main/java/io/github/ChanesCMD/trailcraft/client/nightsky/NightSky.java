package io.github.ChanesCMD.trailcraft.client.nightsky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Ночное небо: свои цветные звёзды поверх ванильных и падающие звёзды.
 *
 * Звёзды рисуются аддитивными квадами (шейдер position_color), поэтому светятся и не зависят от тумана.
 * Метеоры появляются раз в несколько десятков секунд ночью и летят по небу от 0,6 до 1,2 секунды.
 */
public final class NightSky {
    /** Сколько своих звёзд добавляем поверх ванильных 1500. */
    private static final int STARS = 900;
    private static final float RADIUS = 100.0F;
    /** Яркость, ниже которой звёзды не рисуем (день). */
    private static final float MIN_BRIGHTNESS = 0.02F;
    /** Пауза между метеорами, тиков. */
    private static final int METEOR_MIN_DELAY = 30 * 20, METEOR_MAX_DELAY = 90 * 20;

    /** Своя случайность: не трогаем random мира, иначе сбивается детерминизм генерации. */
    private static final RandomSource RANDOM = RandomSource.create();
    private static List<Star> stars;
    private static final List<Meteor> meteors = new ArrayList<>();
    private static int meteorDelay = METEOR_MIN_DELAY;

    private NightSky() {}

    private record Star(Vector3f pos, float size, float red, float green, float blue, float alpha) {}

    private record Meteor(Vector3f start, Vector3f travel, long bornAtMs, long lifeMs, float size) {}

    private static void buildStars() {
        RandomSource random = RandomSource.create(771077L);
        List<Star> list = new ArrayList<>(STARS);
        // холодные, тёплые и чисто белые звёзды
        float[][] palette = {
                {0.75F, 0.85F, 1.0F}, {1.0F, 1.0F, 1.0F}, {1.0F, 0.93F, 0.78F},
                {0.85F, 0.92F, 1.0F}, {1.0F, 0.82F, 0.70F}, {0.92F, 0.97F, 1.0F},
        };
        while (list.size() < STARS) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 2.0F - 1.0F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            float len = x * x + y * y + z * z;
            if (len <= 0.02F || len >= 1.0F) {
                continue;
            }
            Vector3f pos = new Vector3f(x, y, z).normalize(RADIUS);
            float[] color = palette[random.nextInt(palette.length)];
            float bright = 0.45F + random.nextFloat() * 0.55F;
            list.add(new Star(pos,
                    0.10F + random.nextFloat() * 0.26F + (random.nextFloat() < 0.08F ? 0.18F : 0.0F),
                    color[0] * bright, color[1] * bright, color[2] * bright,
                    0.55F + random.nextFloat() * 0.45F));
        }
        stars = list;
    }

    /** Вызывается каждый клиентский тик: рождает метеоры ночью. */
    public static void tick(ClientLevel level) {
        if (!ClientConfig.NIGHT_SKY.get() || level == null) {
            meteors.clear();
            return;
        }
        if (meteorDelay > 0) {
            meteorDelay--;
            return;
        }
        if (level.getStarBrightness(1.0F) > 0.2F && level.getRainLevel(1.0F) < 0.2F
                && RANDOM.nextInt(3) != 0) {
            spawnMeteor(RANDOM);
            meteorDelay = METEOR_MIN_DELAY + RANDOM.nextInt(METEOR_MAX_DELAY - METEOR_MIN_DELAY);
        } else {
            meteorDelay = 20;
        }
    }

    private static void spawnMeteor(RandomSource random) {
        // начальная точка — случайная сторона неба повыше, летит вниз и в сторону
        float angle = (random.nextFloat() - 0.5F) * 2.4F;
        float height = 0.35F + random.nextFloat() * 0.5F;
        Vector3f start = new Vector3f(Mth.sin(angle), height, Mth.cos(angle)).normalize(RADIUS * 0.95F);
        Vector3f travel = new Vector3f(-Mth.sin(angle) * 0.7F - 0.5F - random.nextFloat() * 0.5F,
                -1.0F - random.nextFloat() * 0.4F,
                -Mth.cos(angle) * 0.7F).normalize();
        meteors.add(new Meteor(start, travel, System.currentTimeMillis(),
                450L + random.nextInt(700), 0.35F + random.nextFloat() * 0.35F));
    }

    /** Рисует свои звёзды и метеоры; вызывается в конце LevelRenderer.renderSky тем же поворотом неба. */
    public static void render(PoseStack pose, Matrix4f projection, float partialTick, ClientLevel level) {
        if (!ClientConfig.NIGHT_SKY.get()) {
            return;
        }
        float brightness = level.getStarBrightness(partialTick) * (1.0F - level.getRainLevel(partialTick));
        if (brightness < MIN_BRIGHTNESS) {
            meteors.clear();
            return;
        }
        if (stars == null) {
            buildStars();
        }
        PoseStack sky = new PoseStack();
        sky.last().pose().set(pose.last().pose());

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = sky.last().pose();
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = new Vector3f();
        Vector3f bitangent = new Vector3f();
        for (Star star : stars) {
            if (Math.abs(star.pos.y) > RADIUS * 0.999F) {
                continue;
            }
            tangent.set(up).cross(star.pos).normalize();
            bitangent.set(star.pos).cross(tangent).normalize();
            float s = star.size();
            float a = star.alpha() * brightness;
            addQuad(bb, matrix, star.pos, tangent, bitangent, s, s, a,
                    star.red(), star.green(), star.blue());
        }
        draw(bb);

        long now = System.currentTimeMillis();
        Iterator<Meteor> it = meteors.iterator();
        if (meteors.isEmpty()) {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            return;
        }
        BufferBuilder streaks = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        while (it.hasNext()) {
            Meteor meteor = it.next();
            float progress = (now - meteor.bornAtMs()) / (float) meteor.lifeMs();
            if (progress >= 1.0F) {
                it.remove();
                continue;
            }
            Vector3f head = new Vector3f(meteor.start()).fma(progress * 70.0F, meteor.travel());
            Vector3f tail = new Vector3f(meteor.start()).fma(Math.max(0.0F, progress - 0.30F) * 70.0F, meteor.travel());
            tangent.set(meteor.travel()).cross(meteor.start()).normalize();
            float a = brightness * (1.0F - progress) * 0.9F;
            addStreak(streaks, matrix, tail, head, tangent, meteor.size(), a);
        }
        draw(streaks);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private static void addQuad(BufferBuilder bb, Matrix4f matrix, Vector3f center, Vector3f t1, Vector3f t2,
                                float w, float h, float alpha, float r, float g, float b) {
        Vector3f a = new Vector3f(center).fma(-w, t1).fma(-h, t2);
        Vector3f c = new Vector3f(center).fma(w, t1).fma(-h, t2);
        Vector3f d = new Vector3f(center).fma(w, t1).fma(h, t2);
        Vector3f e = new Vector3f(center).fma(-w, t1).fma(h, t2);
        for (int i = 0; i < 4; i++) {
            Vector3f v = switch (i) {
                case 0 -> a;
                case 1 -> c;
                case 2 -> d;
                default -> e;
            };
            bb.addVertex(matrix, v.x, v.y, v.z).setColor(r, g, b, alpha);
        }
    }

    private static void addStreak(BufferBuilder bb, Matrix4f matrix, Vector3f tail, Vector3f head, Vector3f perp,
                                  float size, float alpha) {
        Vector3f a = new Vector3f(tail).fma(-size * 0.15F, perp);
        Vector3f c = new Vector3f(tail).fma(size * 0.15F, perp);
        Vector3f d = new Vector3f(head).fma(size * 0.6F, perp);
        Vector3f e = new Vector3f(head).fma(-size * 0.6F, perp);
        bb.addVertex(matrix, a.x, a.y, a.z).setColor(1.0F, 0.95F, 0.85F, 0.0F);
        bb.addVertex(matrix, c.x, c.y, c.z).setColor(1.0F, 0.95F, 0.85F, 0.0F);
        bb.addVertex(matrix, d.x, d.y, d.z).setColor(1.0F, 1.0F, 1.0F, alpha);
        bb.addVertex(matrix, e.x, e.y, e.z).setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    private static void draw(BufferBuilder bb) {
        MeshData mesh = bb.buildOrThrow();
        BufferUploader.drawWithShader(mesh);
    }
}

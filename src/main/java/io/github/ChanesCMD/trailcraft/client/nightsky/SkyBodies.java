package io.github.ChanesCMD.trailcraft.client.nightsky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Луна и планеты на небе. Луна видна и днём, идёт по небу с востока на запад вместе со звёздами (как солнце),
 * только медленно смещается по орбите. Планеты маленькие, у каждой своя орбита; рисуются ночью.
 */
public final class SkyBodies {
    /** Чуть ближе небесного купола (100), иначе тела мерцают в z-fighting с ним. */
    private static final float RADIUS = 90.0F;
    private static final float MOON_SIZE = 3.0F;

    private record Body(ResourceLocation texture, float size, double speed, double phase, double tilt, float dim) {}

    /** Границы светлой части каждой фазы в пикселях клетки (измерены по текстуре), -1 — нет светлой части. */
    private static final int[] LIT_MIN = {1, 1, 1, 1, -1, 7, 5, 3};
    private static final int[] LIT_MAX = {7, 5, 3, 1, -1, 7, 7, 7};
    /** Куда растёт луна: -1 влево (растущая), +1 вправо (убывающая). */
    private static final int[] GROW = {0, 1, 1, 1, 0, -1, -1, -1};

    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "textures/environment/moon_phases_06.png");

    private static Body planet(String name, float size, double speed, double phase, double tilt, float dim) {
        return new Body(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID,
                "textures/environment/planet_" + name + ".png"), size, speed, phase, tilt, dim);
    }

    /**
     * Размеры планет уменьшены вдвое, последнее число — яркость по реальному расстоянию от Земли:
     * чем дальше, тем тусклее (Венера 0,41 а.е., Нептун 30 а.е.).
     */
    private static final Body[] PLANETS = {
            planet("mercury", 0.6F, 0.08, 0.4, 18, 0.95F),
            planet("venus", 0.7F, 0.06, 2.1, 9, 1.0F),
            planet("mars", 0.65F, 0.045, 3.4, 6, 0.82F),
            planet("jupiter", 0.95F, 0.03, 4.7, 14, 0.6F),
            planet("saturn", 0.9F, 0.022, 5.6, 22, 0.5F),
            planet("uranus", 0.75F, 0.015, 1.1, 27, 0.36F),
            planet("neptune", 0.75F, 0.01, 2.8, 31, 0.28F),
    };

    private SkyBodies() {}

    private static void begin() {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void end() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    /** Луна: видна всегда (и днём), цвет приглушается в светлое время, но спрайт не прозрачный. */
    public static void renderMoon(Matrix4f matrix, float partialTick, ClientLevel level, float nightBrightness) {
        double day = (level.getDayTime() + partialTick) / 24000.0;
        int phase = level.getMoonPhase() & 7;
        int u = phase % 4, v = phase / 4;
        // полнолуние — напротив солнца (вся ночь), новолуние — рядом с солнцем (день), серпы — утро и вечер
        double angle = Math.PI + (level.getMoonPhase() & 7) * (Math.PI / 4.0);
        // днём луна бледная и голубоватая, ночью — яркая и почти непрозрачная
        float night = Math.min(1.0F, nightBrightness * 1.6F);
        float alpha = 0.5F + 0.5F * night;
        float red = 0.90F + 0.10F * night;
        float green = 0.94F + 0.06F * night;
        float blue = 1.0F;
        begin();
        // ореолы: прямоугольники цвета луны, торчащие со стороны роста на 1 и 2 пикселя
        int litMin = LIT_MIN[phase];
        int litMax = LIT_MAX[phase];
        if (litMin >= 0) {
            int grow = GROW[phase];
            if (grow > 0) {
                // у убывающей каждый ореол шире на пиксель: ближний 2 px, дальний 2 px
                haloRange(matrix, angle, litMax, litMax + 2, 0.90F, 0.94F, 1.0F, alpha * 0.66F);
                haloRange(matrix, angle, litMax + 2, litMax + 3, 0.90F, 0.94F, 1.0F, alpha * 0.33F);
            } else if (grow < 0) {
                haloRange(matrix, angle, litMin - 1, litMin, 0.90F, 0.94F, 1.0F, alpha * 0.66F);
                haloRange(matrix, angle, litMin - 2, litMin - 1, 0.90F, 0.94F, 1.0F, alpha * 0.33F);
            }
        }
        drawBody(matrix, MOON, u * 0.25F, v * 0.5F, 0.25F, 0.5F, MOON_SIZE, angle, 23.0, red, green, blue, alpha);
        end();
    }

    /** Планеты: только ночью, каждая на своей орбите. */
    public static void renderPlanets(Matrix4f matrix, float partialTick, ClientLevel level, float brightness) {
        if (brightness <= 0.02F) {
            return;
        }
        double day = (level.getDayTime() + partialTick) / 24000.0;
        begin();
        for (Body body : PLANETS) {
            double angle = body.phase() + day * body.speed() * Math.PI * 2.0;
            float dim = Math.min(1.0F, 0.35F + brightness * 0.65F) * body.dim();
            drawBody(matrix, body.texture(), 0.0F, 0.0F, 1.0F, 1.0F,
                    body.size(), angle, body.tilt(), dim, dim, dim, 1.0F);
        }
        end();
    }

    /** Светлый прямоугольник-ореол на месте луны (без текстуры, только цвет и прозрачность). */
    /**
     * Ореол по диапазону пикселей клетки (0..8 включительно): рисуется прямоугольником той же высоты,
     * что и луна, с того же места, но шире — торчит только в сторону роста.
     */
    private static void haloRange(Matrix4f matrix, double angle, int fromPx, int toPx,
                                  float red, float green, float blue, float alpha) {
        double tilt = Math.toRadians(23.0);
        Vector3f pos = new Vector3f(
                (float) (Math.cos(angle) * RADIUS),
                (float) (Math.sin(angle) * Math.cos(tilt) * RADIUS),
                (float) (Math.sin(angle) * Math.sin(tilt) * RADIUS));
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = new Vector3f(up).cross(pos);
        if (tangent.lengthSquared() < 1.0E-4F) {
            tangent.set(1.0F, 0.0F, 0.0F);
        }
        tangent.normalize();
        Vector3f bitangent = new Vector3f(pos).cross(tangent).normalize();
        float px = MOON_SIZE / 4.0F;                   // 4 пикселя от центра до края спрайта
        float a = (fromPx - 4.0F) * px;
        float b = (toPx - 4.0F) * px;
        tangent.mul(1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f p1 = new Vector3f(pos).fma(a, tangent).fma(-MOON_SIZE, bitangent);
        Vector3f p2 = new Vector3f(pos).fma(b, tangent).fma(-MOON_SIZE, bitangent);
        Vector3f p3 = new Vector3f(pos).fma(b, tangent).fma(MOON_SIZE, bitangent);
        Vector3f p4 = new Vector3f(pos).fma(a, tangent).fma(MOON_SIZE, bitangent);
        bb.addVertex(matrix, p1.x, p1.y, p1.z).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, p2.x, p2.y, p2.z).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, p3.x, p3.y, p3.z).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, p4.x, p4.y, p4.z).setColor(red, green, blue, alpha);
        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    }

    private static void halo(Matrix4f matrix, double angle, float halfW, float halfH,
                             float red, float green, float blue, float alpha) {
        double tilt = Math.toRadians(23.0);
        Vector3f pos = new Vector3f(
                (float) (Math.cos(angle) * RADIUS),
                (float) (Math.sin(angle) * Math.cos(tilt) * RADIUS),
                (float) (Math.sin(angle) * Math.sin(tilt) * RADIUS));
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = new Vector3f(up).cross(pos);
        if (tangent.lengthSquared() < 1.0E-4F) {
            tangent.set(1.0F, 0.0F, 0.0F);
        }
        tangent.normalize().mul(halfW);
        Vector3f bitangent = new Vector3f(pos).cross(tangent).normalize().mul(halfH);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.addVertex(matrix, pos.x - tangent.x - bitangent.x, pos.y - tangent.y - bitangent.y, pos.z - tangent.z - bitangent.z)
                .setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x + tangent.x - bitangent.x, pos.y + tangent.y - bitangent.y, pos.z + tangent.z - bitangent.z)
                .setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x + tangent.x + bitangent.x, pos.y + tangent.y + bitangent.y, pos.z + tangent.z + bitangent.z)
                .setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x - tangent.x + bitangent.x, pos.y - tangent.y + bitangent.y, pos.z - tangent.z + bitangent.z)
                .setColor(red, green, blue, alpha);
        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    }

    private static void drawBody(Matrix4f matrix, ResourceLocation texture, float u0, float v0,
                                 float uw, float vh, float size, double angle, double tiltDeg,
                                 float red, float green, float blue, float alpha) {
        double tilt = Math.toRadians(tiltDeg);
        Vector3f pos = new Vector3f(
                (float) (Math.cos(angle) * RADIUS),
                (float) (Math.sin(angle) * Math.cos(tilt) * RADIUS),
                (float) (Math.sin(angle) * Math.sin(tilt) * RADIUS));
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f tangent = new Vector3f(up).cross(pos);
        if (tangent.lengthSquared() < 1.0E-4F) {
            tangent.set(1.0F, 0.0F, 0.0F);
        }
        tangent.normalize();
        Vector3f bitangent = new Vector3f(pos).cross(tangent).normalize();
        tangent.mul(size);
        bitangent.mul(size);

        RenderSystem.setShaderTexture(0, texture);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(matrix, pos.x - tangent.x - bitangent.x, pos.y - tangent.y - bitangent.y, pos.z - tangent.z - bitangent.z)
                .setUv(u0, v0 + vh).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x + tangent.x - bitangent.x, pos.y + tangent.y - bitangent.y, pos.z + tangent.z - bitangent.z)
                .setUv(u0 + uw, v0 + vh).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x + tangent.x + bitangent.x, pos.y + tangent.y + bitangent.y, pos.z + tangent.z + bitangent.z)
                .setUv(u0 + uw, v0).setColor(red, green, blue, alpha);
        bb.addVertex(matrix, pos.x - tangent.x + bitangent.x, pos.y - tangent.y + bitangent.y, pos.z - tangent.z + bitangent.z)
                .setUv(u0, v0).setColor(red, green, blue, alpha);
        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }
}

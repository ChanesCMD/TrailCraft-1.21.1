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
 * Луна и планеты на ночном небе. Спрайты маленькие (как на настоящем небе, чуть крупнее),
 * каждый идёт по своей орбите: у планеты свои скорость, наклон и начальный угол.
 * Ванильную луну заменяет прозрачная текстура (assets/minecraft/.../moon_phases.png).
 */
public final class SkyBodies {
    /** Чуть ближе небесного купола (100), иначе тела мерцают в z-fighting с ним. */
    private static final float RADIUS = 90.0F;
    /** Размер спрайта планеты в единицах неба (ванильная звезда ~0,25). */
    private static final float MOON_SIZE = 2.6F;

    private record Body(ResourceLocation texture, float size, double speed, double phase, double tilt) {}

    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "textures/environment/moon_phases_06.png");

    private static Body planet(String name, float size, double speed, double phase, double tilt) {
        return new Body(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID,
                "textures/environment/planet_" + name + ".png"), size, speed, phase, tilt);
    }

    /** Скорости — оборотов в сутки; луна медленнее всех планет. */
    private static final Body[] PLANETS = {
            planet("mercury", 1.2F, 0.08, 0.4, 18),
            planet("venus", 1.4F, 0.06, 2.1, 9),
            planet("mars", 1.3F, 0.045, 3.4, 6),
            planet("jupiter", 1.9F, 0.03, 4.7, 14),
            planet("saturn", 1.8F, 0.022, 5.6, 22),
            planet("uranus", 1.5F, 0.015, 1.1, 27),
            planet("neptune", 1.5F, 0.01, 2.8, 31),
    };

    private SkyBodies() {}

    /** Рисует луну и планеты той же матрицей неба, что и звёзды. */
    public static void render(Matrix4f matrix, float partialTick, ClientLevel level, float brightness) {
        if (brightness <= 0.02F) {
            return;
        }
        double day = (level.getDayTime() + partialTick) / 24000.0;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // луна — восьмая из листа (4x2), как у ванили
        int phase = level.getMoonPhase() & 7;
        int u = phase % 4, v = phase / 4;
        drawBody(matrix, MOON, u * 0.25F, v * 0.5F, 0.25F, 0.5F,
                MOON_SIZE, day * 0.06 + 4.2, 23.0, brightness);
        for (Body body : PLANETS) {
            double angle = body.phase() + day * body.speed() * Math.PI * 2.0;
            drawBody(matrix, body.texture(), 0.0F, 0.0F, 1.0F, 1.0F,
                    body.size(), angle, body.tilt(), brightness);
        }
        // государства рендера возвращаем: иначе после планет мир рисовался без записи глубины
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    private static void drawBody(Matrix4f matrix, ResourceLocation texture, float u0, float v0,
                                 float uw, float vh, float size, double angle, double tiltDeg, float alpha) {
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
                .setUv(u0, v0 + vh).setColor(1.0F, 1.0F, 1.0F, alpha);
        bb.addVertex(matrix, pos.x + tangent.x - bitangent.x, pos.y + tangent.y - bitangent.y, pos.z + tangent.z - bitangent.z)
                .setUv(u0 + uw, v0 + vh).setColor(1.0F, 1.0F, 1.0F, alpha);
        bb.addVertex(matrix, pos.x + tangent.x + bitangent.x, pos.y + tangent.y + bitangent.y, pos.z + tangent.z + bitangent.z)
                .setUv(u0 + uw, v0).setColor(1.0F, 1.0F, 1.0F, alpha);
        bb.addVertex(matrix, pos.x - tangent.x + bitangent.x, pos.y - tangent.y + bitangent.y, pos.z - tangent.z + bitangent.z)
                .setUv(u0, v0).setColor(1.0F, 1.0F, 1.0F, alpha);
        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.weather;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Круг на воде от капли: лежит на поверхности, расходится (4 кадра) и бледнеет. */
public class RainRippleParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    RainRippleParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(5);
        this.quadSize = 0.25f + random.nextFloat() * 0.15f;
        setColor(r, g, b);
        this.alpha = 0.6f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, zo, z) - cam.z());
        float size = quadSize;
        float fade = 1 - (age + partialTicks) / lifetime;
        WeatherQuad.render(buffer, px, py, pz, new Vector3f(size, 0, 0), new Vector3f(0, 0, size),
                getU0(), getU1(), getV0(), getV1(), rCol, gCol, bCol, alpha * Mth.clamp(fade * 1.5f, 0, 1),
                getLightColor(partialTicks));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Скорость при спавне — цвет капли. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double r, double g, double b) {
            return new RainRippleParticle(level, x, y, z, (float) r, (float) g, (float) b, sprites);
        }
    }
}

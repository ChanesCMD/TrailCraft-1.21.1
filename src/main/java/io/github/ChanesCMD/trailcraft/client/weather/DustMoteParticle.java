package io.github.ChanesCMD.trailcraft.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Пыль пыльной бури: несётся по ветру низко над землёй, крутится и подскакивает; появляется и исчезает плавно.
 * Цвет — цвет земли, с которой её подняло (песок жёлтый, красный песок красный).
 */
public class DustMoteParticle extends TextureSheetParticle {
    /** Доля скорости ветра, с которой летит пыль (блоков/тик при силе ветра 1). */
    static final float WIND_SPEED = 0.7f;

    private final float maxAlpha, spin;

    DustMoteParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y, z);
        Vec3 wind = WeatherWind.vector(level, 0);
        this.xd = wind.x * WIND_SPEED * (0.7 + random.nextFloat() * 0.6);
        this.zd = wind.z * WIND_SPEED * (0.7 + random.nextFloat() * 0.6);
        this.yd = random.nextFloat() * 0.02;
        this.gravity = 0.004f;
        this.friction = 0.98f;
        this.hasPhysics = true;
        this.lifetime = 60 + random.nextInt(60);
        this.quadSize = 0.04f + random.nextFloat() * 0.12f;
        float shade = 0.85f + random.nextFloat() * 0.25f;
        setColor(Math.min(1, r * shade), Math.min(1, g * shade), Math.min(1, b * shade));
        this.maxAlpha = 0.5f + random.nextFloat() * 0.4f;
        this.alpha = 0;
        this.spin = (random.nextFloat() - 0.5f) * 0.3f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += spin;
        // ветер подгоняет, вихри подбрасывают
        Vec3 wind = WeatherWind.vector(level, 0);
        xd += (wind.x * WIND_SPEED - xd) * 0.08 + (random.nextFloat() - 0.5f) * 0.02;
        zd += (wind.z * WIND_SPEED - zd) * 0.08 + (random.nextFloat() - 0.5f) * 0.02;
        if (onGround) {
            yd = 0.03 + random.nextFloat() * 0.05 * WeatherWind.strength(level, 0);
        }
        super.tick();
        float life = (float) age / lifetime;
        alpha = maxAlpha * Mth.clamp(Math.min(life * 6, (1 - life) * 4), 0, 1);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Скорость при спавне — цвет земли. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double r, double g, double b) {
            return new DustMoteParticle(level, x, y, z, (float) r, (float) g, (float) b, sprites);
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Брызги от капли на земле: короткая анимация «коронки» (4 кадра), цвет — как у капли. */
public class RainSplashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    RainSplashParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y + 0.06, z);
        this.sprites = sprites;
        this.xd = this.yd = this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 6 + random.nextInt(3);
        this.quadSize = 0.06f + random.nextFloat() * 0.04f;
        setColor(r, g, b);
        this.alpha = 0.7f;
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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Скорость при спавне — цвет капли. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double r, double g, double b) {
            return new RainSplashParticle(level, x, y, z, (float) r, (float) g, (float) b, sprites);
        }
    }
}

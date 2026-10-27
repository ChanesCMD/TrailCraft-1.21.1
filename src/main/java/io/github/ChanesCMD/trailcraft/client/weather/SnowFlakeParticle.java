package io.github.ChanesCMD.trailcraft.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Снежинка: падает медленно, раскачивается и кружится, ветер её сносит. Упав, немного лежит и тает; в воде тает сразу.
 */
public class SnowFlakeParticle extends TextureSheetParticle {
    /** Сколько тиков снежинка лежит на земле. */
    static final int LIE_TICKS = 30;

    private final float swayPhase, swaySpeed, spin;
    private boolean landed;

    SnowFlakeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0;
        this.friction = 1;
        this.hasPhysics = true;
        this.lifetime = 400;
        this.quadSize = 0.035f + random.nextFloat() * 0.045f;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        this.swaySpeed = 0.08f + random.nextFloat() * 0.08f;
        this.spin = (random.nextFloat() - 0.5f) * 0.15f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        this.alpha = 0.85f + random.nextFloat() * 0.15f;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (landed) {
            alpha = Math.max(0, alpha - 1f / LIE_TICKS);
            return;
        }
        float sway = Mth.sin(age * swaySpeed + swayPhase) * 0.012f;
        roll += spin;
        double dx = xd + sway, dy = yd, dz = zd + Mth.cos(age * swaySpeed * 0.7f + swayPhase) * 0.008f;
        move(dx, dy, dz);
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.getFluidState(pos).isEmpty()) {
            remove();
        } else if (onGround || RainDropParticle.blocked(x - xo, y - yo, z - zo, dx, dy, dz)) {
            landed = true;
            lifetime = age + LIE_TICKS;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SnowFlakeParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

package io.github.ChanesCMD.trailcraft.client.weather;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ChanesCMD.trailcraft.init.ModParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Капля дождя: тонкая полоса вдоль скорости (косая на ветру). Цвет — цвет воды биома, разбавленный белым, поэтому
 * над морем дождь светлее. Упав на землю, даёт брызги, в воду — круг, в лаву — пар. Сквозь листву (она проходима)
 * капля проходит не всегда: крона задерживает большую часть, остальные стекают медленной каплей.
 */
public class RainDropParticle extends TextureSheetParticle {
    /** Длина полосы на единицу скорости (блоков на блок/тик), ширина — в блоках. */
    static final float LENGTH = 0.55f, WIDTH = 0.022f;
    /** Какая доля капель застревает в кроне. */
    static final float LEAVES_CATCH = 0.7f;
    /** Дымок от капли в лаве — у одной капли из стольких. */
    static final int LAVA_SMOKE_CHANCE = 6;

    private boolean throughLeaves;

    RainDropParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0;
        this.friction = 1;
        this.hasPhysics = true;
        this.lifetime = 80;
        pickSprite(sprites);
        int water = level.getBiome(BlockPos.containing(x, y, z)).value().getWaterColor();
        this.rCol = mixWhite((water >> 16 & 255) / 255f);
        this.gCol = mixWhite((water >> 8 & 255) / 255f);
        this.bCol = mixWhite((water & 255) / 255f);
        this.alpha = 0.45f + random.nextFloat() * 0.2f;
    }

    static float mixWhite(float c) {
        return 0.45f * c + 0.55f;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        double dx = xd, dy = yd, dz = zd;
        move(dx, dy, dz);
        if (onGround || blocked(x - xo, y - yo, z - zo, dx, dy, dz)) {
            splash(ModParticles.RAIN_SPLASH.get(), y);
            remove();
            return;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isEmpty()) {
            double surface = pos.getY() + fluid.getHeight(level, pos);
            if (y <= surface) {
                if (fluid.is(FluidTags.LAVA)) {
                    if (random.nextInt(LAVA_SMOKE_CHANCE) == 0) {
                        level.addParticle(ParticleTypes.SMOKE, x, surface, z, 0, 0.02, 0);
                    }
                } else {
                    splash(ModParticles.RAIN_RIPPLE.get(), surface + 0.01);
                }
                remove();
                return;
            }
        }
        if (!throughLeaves && level.getBlockState(pos).getBlock() instanceof LeavesBlock) {
            throughLeaves = true;
            if (random.nextFloat() < LEAVES_CATCH) {
                remove();
                return;
            }
            // стекает с листа: медленнее и почти отвесно
            xd *= 0.15;
            zd *= 0.15;
            yd = -0.35 - random.nextFloat() * 0.15;
        }
    }

    /** Частица упёрлась в блок: сдвинулась меньше, чем просили (поле stoppedByCollision у Particle закрыто). */
    static boolean blocked(double movedX, double movedY, double movedZ, double dx, double dy, double dz) {
        return Math.abs(movedX - dx) > 1.0E-5 || Math.abs(movedY - dy) > 1.0E-5 || Math.abs(movedZ - dz) > 1.0E-5;
    }

    private void splash(SimpleParticleType type, double atY) {
        Minecraft.getInstance().particleEngine.createParticle(type, x, atY, z, rCol, gCol, bCol);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, zo, z) - cam.z());
        Vector3f dir = new Vector3f((float) xd, (float) yd, (float) zd);
        float speed = dir.length();
        if (speed < 1.0E-4f) {
            return;
        }
        dir.div(speed);
        // полуось вдоль капли (верх текстуры — хвост, против движения) и поперёк неё, к камере
        Vector3f along = new Vector3f(dir).mul(-LENGTH * speed * 0.5f);
        Vector3f view = new Vector3f(px, py, pz);
        Vector3f side = dir.cross(view, new Vector3f());
        if (side.lengthSquared() < 1.0E-6f) {
            return;
        }
        side.normalize(WIDTH);
        // голова капли — в точке частицы, центр полосы — на полдлины позади
        WeatherQuad.render(buffer, px + along.x, py + along.y, pz + along.z, side, along,
                getU0(), getU1(), getV0(), getV1(), rCol, gCol, bCol, alpha, getLightColor(partialTicks));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new RainDropParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}

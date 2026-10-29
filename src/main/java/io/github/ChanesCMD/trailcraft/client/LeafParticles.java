package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Падающая листва, как у ванильной вишни: с листвы сакуры — розовые лепестки, с ивы — зелёные листья. Частица — ванильная
 * частица лепестка вишни (падает, кружится, ложится на землю) со своими текстурами (scripts/gen_leaf_particles.py).
 */
public final class LeafParticles {
    /** Раз в сколько тиков отображения блока листвы падает лепесток (у ванильной вишни — 10). */
    private static final int SAKURA_CHANCE = 10, WILLOW_CHANCE = 24;
    private static final ResourceLocation SAKURA = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "sakura_leaves");
    private static final ResourceLocation WILLOW = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "willow_leaves");
    private static Block sakura, willow;

    private LeafParticles() {}

    public static void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (sakura == null) {
            sakura = BuiltInRegistries.BLOCK.get(SAKURA);
            willow = BuiltInRegistries.BLOCK.get(WILLOW);
        }
        if (state.is(sakura)) {
            fall(level, pos, random, SAKURA_CHANCE, ModParticles.SAKURA_PETAL.get());
        } else if (state.is(willow)) {
            fall(level, pos, random, WILLOW_CHANCE, ModParticles.WILLOW_LEAF.get());
        }
    }

    /** Как CherryLeavesBlock.animateTick: под листвой не должно быть полного блока. */
    private static void fall(Level level, BlockPos pos, RandomSource random, int chance, ParticleOptions particle) {
        if (random.nextInt(chance) != 0) {
            return;
        }
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        if (!Block.isFaceFull(under.getCollisionShape(level, below), Direction.UP)) {
            ParticleUtils.spawnParticleBelow(level, pos, random, particle);
        }
    }

    /** Ванильный лепесток вишни (конструктор у него protected). */
    private static class Leaf extends CherryParticle {
        Leaf(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites);
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new Leaf(level, x, y, z, sprites);
        }
    }
}

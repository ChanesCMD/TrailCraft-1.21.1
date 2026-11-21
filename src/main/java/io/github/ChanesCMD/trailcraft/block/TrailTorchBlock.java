package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Наш факел: горит слабее ванильного (уровень света 10 вместо 14), палка из костра. */
public class TrailTorchBlock extends TorchBlock {
    public static final MapCodec<TrailTorchBlock> CODEC = simpleCodec(properties -> new TrailTorchBlock(properties));

    public TrailTorchBlock(BlockBehaviour.Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    public MapCodec<? extends TorchBlock> codec() {
        return CODEC;
    }
}

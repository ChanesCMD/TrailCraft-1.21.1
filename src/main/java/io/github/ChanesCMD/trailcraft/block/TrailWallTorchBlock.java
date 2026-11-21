package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Наш настенный факел: тот же слабый свет, что и у стоячего (10 вместо 14). */
public class TrailWallTorchBlock extends WallTorchBlock {
    public TrailWallTorchBlock(BlockBehaviour.Properties properties) {
        super(ParticleTypes.FLAME, properties);
    }

    @Override
    public String getDescriptionId() {
        return "block.trailcraft.torch";
    }
}

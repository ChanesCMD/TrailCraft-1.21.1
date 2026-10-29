package io.github.ChanesCMD.trailcraft.pipe;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;

/**
 * Лава в трубах Create: обычная медная труба (и стеклянная, и в корпусе), по которой течёт лава, дымит и через
 * BURN_TICKS прогорает — пропадает без дропа. Укреплённая (ReinforcedPipeBlock), помпы, вентили и умные трубы держат.
 * Вызывается из mixin/FluidTransportBehaviourMixin каждый тик трубы на сервере.
 */
public final class LavaPipes {
    /** Сколько тиков лавы выдерживает обычная труба (20 с). */
    public static final int BURN_TICKS = 20 * 20;

    /** Новое значение счётчика нагрева трубы; -1 — труба прогорела (её тик дальше не идёт). */
    public static int tick(FluidTransportBehaviour transport, int heat) {
        if (!(transport.getWorld() instanceof ServerLevel level) || transport.interfaces == null) return heat;
        BlockPos pos = transport.getPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!burns(block)) return 0;
        boolean lava = false;
        for (PipeConnection connection : transport.interfaces.values())
            if (connection != null && connection.getProvidedFluid().getFluid().is(FluidTags.LAVA)) lava = true;
        if (!lava) return Math.max(0, heat - 2);
        heat++;
        if (heat % 10 == 0)
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    1 + 3 * heat / BURN_TICKS, 0.2, 0.1, 0.2, 0.01);
        if (heat > BURN_TICKS / 2 && heat % 40 == 0)
            level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.2f);
        if (heat >= BURN_TICKS) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
            return -1;
        }
        return heat;
    }

    /** Прогорает ли эта труба от лавы. */
    public static boolean burns(Block block) {
        return block instanceof FluidPipeBlock && !(block instanceof ReinforcedPipeBlock)
                || block instanceof GlassFluidPipeBlock || block instanceof EncasedPipeBlock;
    }

    private LavaPipes() {}
}

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.drain.ItemDrainBlock;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.spout.SpoutBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import io.github.ChanesCMD.trailcraft.init.ModReinforced;

/**
 * Лава в жидкостных деталях Create: медная труба (и стеклянная, и в корпусе), помпа, умная труба, вентиль, по которым
 * течёт лава, и шкив шланга, носик, слив, интерфейс жидкостей, бак, в которых она есть, дымят и через BURN_TICKS
 * прогорают — пропадают без дропа. Укреплённые (ReinforcedPipeBlock, init/ModReinforced) держат.
 * Трубы, помпы, вентили — из mixin/FluidTransportBehaviourMixin, остальное — из mixin/SmartBlockEntityMixin, каждый тик
 * на сервере.
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
        return heat(level, pos, heat, lava);
    }

    /** Нагрев детали, в которой лежит жидкость (шкив, носик, слив, интерфейс, бак): лава ли в ней. */
    public static int tickContainer(BlockEntity be, int heat) {
        if (!(be.getLevel() instanceof ServerLevel level) || !burnsHolding(be.getBlockState().getBlock())) return 0;
        BlockPos pos = be.getBlockPos();
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        boolean lava = false;
        if (handler != null)
            for (int i = 0; i < handler.getTanks(); i++)
                if (handler.getFluidInTank(i).getFluid().is(FluidTags.LAVA)) lava = true;
        return heat(level, pos, heat, lava);
    }

    private static int heat(ServerLevel level, BlockPos pos, int heat, boolean lava) {
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

    /** Прогорает ли от лавы эта труба (или помпа, умная труба, вентиль). */
    public static boolean burns(Block block) {
        return (block instanceof FluidPipeBlock || block instanceof GlassFluidPipeBlock || block instanceof EncasedPipeBlock)
                && !ReinforcedPipes.isReinforcedPipe(block)
                || (block instanceof PumpBlock || block instanceof SmartFluidPipeBlock || block instanceof FluidValveBlock)
                && !ModReinforced.isReinforced(block);
    }

    /** Прогорает ли от лавы внутри шкив шланга, носик, слив, интерфейс жидкостей или бак. */
    public static boolean burnsHolding(Block block) {
        // творческий бак не прогорает; интерфейс предметов (тот же класс) жидкость не держит
        return (block instanceof HosePulleyBlock || block instanceof SpoutBlock || block instanceof ItemDrainBlock
                || block instanceof FluidTankBlock && block != AllBlocks.CREATIVE_FLUID_TANK.get()
                || block == AllBlocks.PORTABLE_FLUID_INTERFACE.get())
                && !ModReinforced.isReinforced(block);
    }

    private LavaPipes() {}
}

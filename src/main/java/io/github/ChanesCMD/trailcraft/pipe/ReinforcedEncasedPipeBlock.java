package io.github.ChanesCMD.trailcraft.pipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;

/** Укреплённая труба в корпусе — труба Create в медном корпусе (корпус ставят и снимают так же), держит лаву. */
public class ReinforcedEncasedPipeBlock extends EncasedPipeBlock {
    public ReinforcedEncasedPipeBlock(Properties properties) {
        super(properties, AllBlocks.COPPER_CASING::get);
    }
}

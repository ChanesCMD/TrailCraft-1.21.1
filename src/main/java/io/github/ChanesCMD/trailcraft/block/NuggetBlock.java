package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.NuggetMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Самородок на земле: лежит и подбирается как кремень (PebbleBlock). ВРЕМЕННО — с блок-сущностью, чтобы клиент рисовал
 * над ним столб высотой 40 блоков (client/NuggetMarkerRenderer) и было видно, где они появляются; потом сущность убрать.
 */
public class NuggetBlock extends PebbleBlock implements EntityBlock {
    public static final MapCodec<NuggetBlock> CODEC = simpleCodec(NuggetBlock::new);

    public NuggetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuggetMarkerBlockEntity(pos, state);
    }
}

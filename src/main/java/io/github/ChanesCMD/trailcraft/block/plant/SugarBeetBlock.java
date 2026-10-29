package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BeetrootBlock;

import java.util.function.Supplier;

/** Сахарная свёкла: посевы о 4 стадиях, как ванильная свёкла, со своими семенами. Из корнеплода — сахар. */
public class SugarBeetBlock extends BeetrootBlock {
    private final Supplier<? extends ItemLike> seeds;

    public SugarBeetBlock(Supplier<? extends ItemLike> seeds, Properties properties) {
        super(properties);
        this.seeds = seeds;
    }

    @Override
    public MapCodec<BeetrootBlock> codec() {
        return BeetrootBlock.CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }
}

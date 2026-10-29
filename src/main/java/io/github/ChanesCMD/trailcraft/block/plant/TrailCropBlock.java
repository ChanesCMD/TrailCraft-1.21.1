package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

/** Свои посевы о 8 стадиях (пшеница): как ванильные, сажаются своими семенами на грядку. */
public class TrailCropBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seeds;

    public TrailCropBlock(Supplier<? extends ItemLike> seeds, Properties properties) {
        super(properties);
        this.seeds = seeds;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CropBlock.CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }
}

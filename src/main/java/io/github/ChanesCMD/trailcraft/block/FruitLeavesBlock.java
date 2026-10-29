package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.climate.CropClimate;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.farm.Farm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Листва фруктового дерева: деревья растут без плодов, а природная листва (не поставленная игроком и не опадающая)
 * время от времени завязывает под собой плод (FruitBlock), если снизу пусто и рядом нет других плодов. Числа — farm/Farm.
 */
public class FruitLeavesBlock extends LeavesBlock {
    public static final MapCodec<FruitLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("fruit").forGetter(b -> b.fruit.get()),
            propertiesCodec()
    ).apply(instance, (fruit, props) -> new FruitLeavesBlock(props, () -> fruit)));

    private final Supplier<? extends Block> fruit;

    public FruitLeavesBlock(Properties properties, Supplier<? extends Block> fruit) {
        super(properties);
        this.fruit = fruit;
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    /** Ванильная листва тикает, только когда опадает; фруктовой тик нужен ещё и для плодов. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PERSISTENT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random); // опадающая листва (далеко от ствола) исчезает
        if (state.getValue(PERSISTENT) || state.getValue(DISTANCE) >= 7 || random.nextInt(Farm.FRUIT_SET_CHANCE) != 0) {
            return;
        }
        BlockPos below = pos.below();
        if (level.isEmptyBlock(below) && !fruitNearby(level, below) && CropClimate.canGrow(level, pos, this)) {
            level.setBlock(below, fruit.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private boolean fruitNearby(ServerLevel level, BlockPos pos) {
        int r = Farm.FRUIT_SPACING;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
            if (level.getBlockState(p).getBlock() instanceof FruitBlock) {
                return true;
            }
        }
        return false;
    }
}

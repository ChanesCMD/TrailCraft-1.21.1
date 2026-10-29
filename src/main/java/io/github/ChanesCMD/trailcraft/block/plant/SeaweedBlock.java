package io.github.ChanesCMD.trailcraft.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Водоросль: растёт на дне рек и моря (как ванильная трава морская, но своя и без высокой формы). Сушится на костре —
 * сушёная водоросль (ремни, воронки, туннели Create), 9 сушёных — блок (шкивы).
 */
public class SeaweedBlock extends SeagrassBlock {
    public static final MapCodec<SeagrassBlock> CODEC = simpleCodec(SeaweedBlock::new);

    public SeaweedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SeagrassBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }
}

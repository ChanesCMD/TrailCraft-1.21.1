package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Лёд по берегам: у мелкого блока в воде (камешек, кремень, ветка, самородок, рогоз) зимой замерзает клетка.
 * Состояние frozen рисует тонкую ледяную корку и даёт по ней ходить, как по обычному льду.
 */
public final class ShoreIce {
    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");
    /** Верхние 2 пикселя — по ним ходят. */
    public static final VoxelShape SHEET = Shapes.box(0.0, 0.875, 0.0, 1.0, 1.0, 1.0);

    /** Замерзает там же, где ляжет снег: по осадкам биома (зимой они снежные). */
    public static boolean shouldFreeze(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }

    public static boolean frozen(BlockState state) {
        return state.hasProperty(FROZEN) && state.getValue(FROZEN);
    }

    private ShoreIce() {}
}

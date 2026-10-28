package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.farm.Watering;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Грядка из почвы (жёлтозём, бурозём, чернозём). Вспахивается тяпкой из голой почвы или почвы с дёрном (FarmEvents).
 * Влажная, если в 4 блоках вода, идёт дождь или её полили лейкой (на Farm.WATERED_DAYS игровых суток, farm/Watering);
 * без воды сохнет, но остаётся грядкой — только растения на сухой грядке не растут (FarmEvents). Своей почвой
 * становится, если на неё прыгнуть или поставить сверху блок.
 * Растения на ней растут с множителем growthSpeed (числа — в farm/Farm): больше 1 — грядка даёт растению
 * лишние случайные тики, меньше 1 — часть попыток роста отменяет FarmEvents.
 */
public class GardenBedBlock extends FarmBlock {
    private final Supplier<? extends Block> soil;
    private final float growthSpeed;

    public GardenBedBlock(Properties properties, Supplier<? extends Block> soil, float growthSpeed) {
        super(properties);
        this.soil = soil;
        this.growthSpeed = growthSpeed;
    }

    public float growthSpeed() {
        return growthSpeed;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? defaultBlockState() : soil.get().defaultBlockState();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            dry(null, state, level, pos);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (!isNearWater(level, pos) && !level.isRainingAt(pos.above()) && !Watering.isWatered(level, pos)) {
            // Сохнет постепенно; сухая остаётся грядкой, только растения на ней не растут (FarmEvents)
            if (moisture == 0) {
                return;
            }
            level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
        } else if (moisture < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
        }
        // Быстрая почва: растение сверху получает лишние случайные тики (дробная часть — с такой вероятностью)
        BlockPos above = pos.above();
        for (float extra = growthSpeed - 1; extra > 0; extra--) {
            BlockState plant = level.getBlockState(above);
            if (plant.isRandomlyTicking() && random.nextFloat() < extra) {
                plant.randomTick(level, above, random);
            }
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide && CommonHooks.onFarmlandTrample(level, pos, soil.get().defaultBlockState(), fallDistance, entity)) {
            dry(entity, state, level, pos);
        }
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    /** Сухая грядка (влажность 0) — растения на ней не растут. */
    public static boolean isDry(BlockState state) {
        return state.getValue(MOISTURE) == 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            Watering.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Грядку затоптали или сверху поставили блок — снова своя почва. */
    private void dry(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState soilState = pushEntitiesUp(state, soil.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, soilState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, soilState));
    }

    /** Как у ванильной грядки: вода в 4 блоках по сторонам и на блок выше. */
    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (state.canBeHydrated(level, pos, level.getFluidState(p), p)) {
                return true;
            }
        }
        return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
    }
}

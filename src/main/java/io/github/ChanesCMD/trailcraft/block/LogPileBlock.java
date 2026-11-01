package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.LogPileBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Поленница, как в TFC: 16 слотов 4×4 (LogPileBlockEntity), в слот — 4 толстых или 16 тонких бревна. Блок сам невидим,
 * брёвна рисует client/LogPileRenderer. Создаётся Shift + ПКМ бревном по верхней грани блока (LogPileEvents),
 * дальше: ПКМ бревном — сложить всё, что влезет; Shift + ПКМ бревном — одно; пустой рукой ПКМ — взять одно бревно
 * из слота, на который смотришь, Shift + ПКМ — весь слот. Пустая поленница исчезает, из сломанной брёвна выпадают.
 */
public class LogPileBlock extends BaseEntityBlock {
    public static final MapCodec<LogPileBlock> CODEC = simpleCodec(LogPileBlock::new);
    /** Вдоль какой оси лежат брёвна. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public LogPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!LogPileBlockEntity.accepts(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
            if (pile.room(stack, stack.getCount()) <= 0) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;  // всё занято
            }
            if (!level.isClientSide) {
                int moved = pile.insert(stack, stack.getCount());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(moved);
                }
                level.playSound(null, pos, state.getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
            Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            int slot = pile.nearestFilled(LogPileBlockEntity.slotAt(local, state.getValue(AXIS)));
            ItemStack taken = slot < 0 ? ItemStack.EMPTY
                    : pile.take(slot, player.isSecondaryUseActive() ? LogPileBlockEntity.THIN_CAPACITY : 1);
            if (!taken.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, taken);
                level.playSound(null, pos, state.getSoundType().getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
            }
            if (pile.isEmpty()) {
                level.removeBlock(pos, false);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) {
            pile.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogPileBlockEntity(pos, state);
    }
}

package io.github.ChanesCMD.trailcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * Кровать из соломы: только точка возрождения, спать на ней нельзя (ночь не пропускается). ПКМ — точка возрождения
 * здесь (как у ванильной кровати, не принудительная: сломали кровать — точка пропадает). Где ванильная кровать
 * взрывается (ад, край), точку не ставит. Встать после возрождения — рядом или сверху (как у якоря возрождения).
 */
public class StrawBedBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);

    public StrawBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!BedBlock.canSetSpawn(level)) {
            player.displayClientMessage(Component.translatable("block.trailcraft.straw_bed.no_spawn"), true);
        } else if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setRespawnPosition(level.dimension(), pos, player.getYRot(), false, true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader level,
                                                                     BlockPos pos, float orientation) {
        if (!(level instanceof Level world) || !BedBlock.canSetSpawn(world)) {
            return Optional.empty();
        }
        return RespawnAnchorBlock.findStandUpPosition(type, level, pos).map(p -> ServerPlayer.RespawnPosAngle.of(p, pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

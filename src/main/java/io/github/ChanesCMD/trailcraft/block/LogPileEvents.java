package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.entity.LogPileBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Shift + ПКМ бревном: по верхней грани блока — ставит поленницу с этим бревном, по поленнице — докладывает одно бревно
 * (при Shift ванильное использование блока пропускается, поэтому это не сделать в самом блоке; без Shift поленница
 * принимает брёвна сама — LogPileBlock.useItemOn).
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class LogPileEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.isSecondaryUseActive() || !LogPileBlockEntity.accepts(stack)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState target = level.getBlockState(pos);
        if (target.getBlock() instanceof LogPileBlock) {
            if (!(level.getBlockEntity(pos) instanceof LogPileBlockEntity pile) || pile.room(stack, 1) <= 0) {
                return;
            }
            if (!level.isClientSide) {
                pile.insert(stack, 1);
                consume(player, stack);
                level.playSound(null, pos, target.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            finish(event, level);
            return;
        }
        BlockPos place = pos.above();
        BlockState pileState = ModBlocks.LOG_PILE.get().defaultBlockState()
                .setValue(LogPileBlock.AXIS, player.getDirection().getAxis());
        if (event.getFace() != Direction.UP || !target.isFaceSturdy(level, pos, Direction.UP)
                || !level.getBlockState(place).canBeReplaced() || !level.isUnobstructed(pileState, place, CollisionContext.of(player))
                || !player.mayUseItemAt(place, Direction.UP, stack)) {
            return;
        }
        if (!level.isClientSide) {
            level.setBlock(place, pileState, 3);
            if (level.getBlockEntity(place) instanceof LogPileBlockEntity pile) {
                pile.insert(stack, 1);
            }
            consume(player, stack);
            level.playSound(null, place, pileState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        finish(event, level);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event, Level level) {
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }

    private LogPileEvents() {}
}

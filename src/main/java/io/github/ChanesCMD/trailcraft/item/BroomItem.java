package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.SnowCover;
import io.github.ChanesCMD.trailcraft.season.SweptSnow;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

/**
 * Метла: ПКМ по снегу (или по блоку, на котором он лежит) — смести слой снега целиком. Сметённый столбец запоминается
 * (season/SweptSnow), и снегопад туда больше не кладёт снег. Shift + ПКМ по такому месту возвращает снегу возможность
 * падать. Пока метла в руке, сметённые места подсвечены (season/BroomEvents, client/SweptSnowOverlay).
 */
public class BroomItem extends Item {
    public BroomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return restore(level, pos, player);
        }
        BlockState clicked = level.getBlockState(pos);
        if (!hasSnow(clicked)) {
            // кликнули по блоку под снегом (или по верхней половине двойного растения, снег лежит в нижней)
            boolean upperHalf = clicked.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && clicked.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
            pos = upperHalf ? pos.below() : pos.above();
            if (!hasSnow(level.getBlockState(pos))) {
                return InteractionResult.PASS;
            }
        }
        if (level instanceof ServerLevel server) {
            BlockState snow = server.getBlockState(pos);
            if (snow.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && snow.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();  // снег считается по нижней половине двойного растения
                snow = server.getBlockState(pos);
            }
            if (SnowCover.has(snow)) {
                SnowCover.set(server, pos, snow, 0);  // блок остаётся, снег с него сметён
                snow = Blocks.SNOW.defaultBlockState();
            } else {
                server.removeBlock(pos, false);
            }
            SweptSnow.mark(server, pos);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, snow),
                    pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.05);
            server.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Снег лежит слоем (Blocks.SNOW) или в клетке с травой. */
    private static boolean hasSnow(BlockState state) {
        return state.is(Blocks.SNOW) || SnowCover.has(state);
    }

    /** Shift + ПКМ: снимает метку «сметено» со столбца, снег там снова может падать. */
    private static InteractionResult restore(Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            if (SweptSnow.unmark(server, pos)) {
                server.sendParticles(ParticleTypes.SNOWFLAKE,
                        pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 14, 0.3, 0.2, 0.3, 0.02);
                server.playSound(null, pos, SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                player.displayClientMessage(Component.translatable("message.trailcraft.broom.restored"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.trailcraft.broom.not_swept"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.broom").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.trailcraft.broom.restore").withStyle(ChatFormatting.GRAY));
    }
}

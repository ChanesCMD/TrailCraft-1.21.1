package io.github.ChanesCMD.trailcraft.item;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Метла: ПКМ по снегу (или по блоку, на котором он лежит) — смести слой снега целиком. Сметённый столбец запоминается
 * (season/SweptSnow), и снегопад туда больше не кладёт снег.
 */
public class BroomItem extends Item {
    public BroomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.SNOW)) {
            pos = pos.above();  // кликнули по блоку под снегом
            if (!level.getBlockState(pos).is(Blocks.SNOW)) {
                return InteractionResult.PASS;
            }
        }
        if (level instanceof ServerLevel server) {
            BlockState snow = server.getBlockState(pos);
            server.removeBlock(pos, false);
            SweptSnow.mark(server, pos);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, snow),
                    pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.05);
            server.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.broom").withStyle(ChatFormatting.GRAY));
    }
}

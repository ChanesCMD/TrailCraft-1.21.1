package io.github.ChanesCMD.trailcraft.pipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Укреплённая труба: труба Create из прочных листов, лава её не прожигает (LavaPipes). Ключом в стеклянную трубу не
 * превращается и в корпус не одевается — иначе стала бы обычной трубой Create.
 */
public class ReinforcedPipeBlock extends FluidPipeBlock {
    public static final MapCodec<ReinforcedPipeBlock> CODEC = simpleCodec(ReinforcedPipeBlock::new);

    public ReinforcedPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return tryRemoveBracket(context) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.trailcraft.reinforced_fluid_pipe.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected @NotNull MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }
}

package io.github.ChanesCMD.trailcraft.pipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.PipeBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Укреплённая труба: труба Create из прочных листов, лава её не прожигает (LavaPipes). Ведёт себя как труба Create:
 * ключом — в укреплённую стеклянную (ReinforcedGlassPipeBlock), медным корпусом — в укреплённую трубу в корпусе
 * (ReinforcedEncasedPipeBlock); куда превращаться, подменяет mixin/ReinforcedPipeMixins (ReinforcedPipes.swap).
 */
public class ReinforcedPipeBlock extends FluidPipeBlock {
    public static final MapCodec<ReinforcedPipeBlock> CODEC = simpleCodec(ReinforcedPipeBlock::new);

    public ReinforcedPipeBlock(Properties properties) {
        super(properties);
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

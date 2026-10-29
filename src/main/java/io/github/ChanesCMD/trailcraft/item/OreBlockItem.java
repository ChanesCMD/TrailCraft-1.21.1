package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.OreBlock;
import io.github.ChanesCMD.trailcraft.ore.Rock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;

import java.util.List;

/** Предмет руды. Порода — в компоненте block_state (без него ставится первая порода руды); в подсказке пишется порода. */
public class OreBlockItem extends BlockItem {
    public OreBlockItem(OreBlock block, Properties properties) {
        super(block, properties);
    }

    /** Порода, в которой поставится руда. */
    public static Rock rock(ItemStack stack) {
        OreBlock block = (OreBlock) ((BlockItem) stack.getItem()).getBlock();
        BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
        Rock rock = props == null ? null : props.get(block.ore.rockProperty);
        return rock == null ? block.ore.rocks.get(0) : rock;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component rockName = rock(stack).block().getName();
        tooltip.add(Component.translatable("tooltip.trailcraft.ore.rock", rockName).withStyle(ChatFormatting.GRAY));
    }
}

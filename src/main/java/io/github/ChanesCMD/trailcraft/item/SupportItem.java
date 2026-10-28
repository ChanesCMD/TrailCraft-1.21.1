package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.SupportBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Крепь: ставит стойку, а ПКМ по боку другой крепи (или где стойке не на чем стоять) — перекладину. С перекладины
 * выпадает этот же предмет.
 */
public class SupportItem extends BlockItem {
    private final SupportBlock post, beam;

    public SupportItem(SupportBlock post, SupportBlock beam, Properties properties) {
        super(post, properties);
        this.post = post;
        this.beam = beam;
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        BlockState postState = post.connected(level, pos);
        BlockState beamState = beam.connected(level, pos);
        var face = context.getClickedFace();
        boolean onSupportSide = face.getAxis().isHorizontal()
                && level.getBlockState(pos.relative(face.getOpposite())).getBlock() instanceof SupportBlock;
        for (BlockState state : onSupportSide ? List.of(beamState, postState) : List.of(postState, beamState)) {
            if (canPlace(context, state)) {
                return state;
            }
        }
        return null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(beam, item);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int size = post.horizontal * 2 + 1;
        tooltip.add(Component.translatable("item.trailcraft.support.range", size, post.vertical * 2 + 1, size)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.trailcraft.support.beam", SupportBlock.MAX_SPAN).withStyle(ChatFormatting.GRAY));
    }
}

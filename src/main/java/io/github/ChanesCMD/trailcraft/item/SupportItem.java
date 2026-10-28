package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.SupportBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Опора: ставит стойку, а ПКМ по боку другой опоры (или где стойке не на чем стоять) — перекладину. С перекладины
 * выпадает этот же предмет. Стойка ставится столбом до {@link #COLUMN} блоков вверх, пока есть место и предметы; если
 * на высоте верха столба по прямой через не больше {@link SupportBlock#MAX_GAP} свободных блоков стоит другая стойка,
 * между ними сама ставится перекладина (тоже если хватает предметов) — получается рама.
 */
public class SupportItem extends BlockItem {
    /** Сколько стоек ставится сразу одна над другой. */
    public static final int COLUMN = 3;

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
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (result.consumesAction() && !level.isClientSide && level.getBlockState(pos).is(post)) {
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            boolean infinite = player != null && player.getAbilities().instabuild;
            // столб: ещё стойки над поставленной, пока есть место и предметы
            BlockPos top = pos;
            for (int i = 1; i < COLUMN && (infinite || !stack.isEmpty()); i++) {
                BlockPos above = pos.above(i);
                if (!isFree(level, above, post.defaultBlockState())) {
                    break;
                }
                level.setBlockAndUpdate(above, post.connected(level, above));
                top = above;
                if (!infinite) {
                    stack.shrink(1);
                }
            }
            // перекладины до соседних стоек на высоте верха столба
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                List<BlockPos> gap = gapTo(level, top, dir);
                if (gap != null && !gap.isEmpty() && (infinite || stack.getCount() >= gap.size())) {
                    for (BlockPos p : gap) {
                        level.setBlockAndUpdate(p, beam.connected(level, p));
                    }
                    if (!infinite) {
                        stack.shrink(gap.size());
                    }
                }
            }
            if (top != pos) {
                var sound = post.defaultBlockState().getSoundType(level, top, player);
                level.playSound(null, top, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
            }
        }
        return result;
    }

    /** Свободные блоки от стойки from до другой стойки в направлении dir; null — стойки нет или путь занят. */
    @Nullable
    private List<BlockPos> gapTo(Level level, BlockPos from, Direction dir) {
        List<BlockPos> gap = new ArrayList<>();
        for (int i = 1; i <= SupportBlock.MAX_GAP + 1; i++) {
            BlockPos p = from.relative(dir, i);
            BlockState state = level.getBlockState(p);
            if (SupportBlock.isPost(state)) {
                return gap;
            }
            if (!isFree(level, p, beam.defaultBlockState())) {
                return null;
            }
            gap.add(p);
        }
        return null;
    }

    private static boolean isFree(Level level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()
                && level.isUnobstructed(state, pos, CollisionContext.empty());
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(beam, item);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int height = post.vertical * 2 + 1;
        int frame = post.reach(true) * 2 + 1, lone = post.reach(false) * 2 + 1;
        tooltip.add(Component.translatable("item.trailcraft.support.frame", frame, height, frame).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.trailcraft.support.lone", lone, height, lone).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.trailcraft.support.column", COLUMN, SupportBlock.MAX_GAP).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.trailcraft.support.beam").withStyle(ChatFormatting.GRAY));
    }
}

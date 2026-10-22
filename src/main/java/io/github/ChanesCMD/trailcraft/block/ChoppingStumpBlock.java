package io.github.ChanesCMD.trailcraft.block;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModWood;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Пень для рубки. На него ставят бревно (обычное или обтёсанное) и бьют топором:
 * бревно раскалывается на 2 тонких бревна той же породы.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class ChoppingStumpBlock extends Block {
    public static final MapCodec<ChoppingStumpBlock> CODEC = simpleCodec(ChoppingStumpBlock::new);
    /** Широкое основание 3 пикселя и столб 14x14 до самого верха, чтобы бревно лежало вплотную. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(1, 3, 1, 15, 16, 15));

    private static Map<Block, ModWood.WoodSet> logToSet;

    public ChoppingStumpBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static ModWood.WoodSet woodOf(Block block) {
        if (logToSet == null) {
            logToSet = new HashMap<>();
            for (ModWood.WoodSet set : ModWood.SETS) {
                logToSet.put(set.log.get(), set);
                logToSet.put(set.strippedLog.get(), set);
            }
        }
        return logToSet.get(block);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos.below()).is(ModBlocks.CHOPPING_STUMP.get())) {
            return;
        }
        BlockState log = level.getBlockState(pos);
        ModWood.WoodSet set = woodOf(log.getBlock());
        ItemStack tool = event.getItemStack();
        if (set == null || !tool.is(ItemTags.AXES)) {
            return;
        }
        event.setCanceled(true);
        level.levelEvent(2001, pos, Block.getId(log)); // частицы и звук разрушения
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 0.8f);
        Block.popResource(level, pos, new ItemStack(set.thinLog.get(), 2));
        tool.hurtAndBreak(1, event.getEntity(), EquipmentSlot.MAINHAND);
    }
}

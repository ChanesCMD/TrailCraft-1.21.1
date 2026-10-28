package io.github.ChanesCMD.trailcraft.farm;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.GardenBedBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import java.util.Map;

/** Вспахивание почв в грядки, рост только на влажной грядке и медленный рост на жёлтозёме. Сами грядки — block/GardenBedBlock. */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class FarmEvents {
    private static Map<Block, Block> bedBySoil;

    /** Грядка, в которую вспахивается блок (почва или почва с дёрном), или null. */
    private static Block bedFor(Block block) {
        if (bedBySoil == null) {
            bedBySoil = Map.of(
                    ModBlocks.ZHELTOZEM.get(), ModBlocks.ZHELTOZEM_FARMLAND.get(),
                    ModBlocks.ZHELTOZEM_GRASS.get(), ModBlocks.ZHELTOZEM_FARMLAND.get(),
                    ModBlocks.BUROZEM.get(), ModBlocks.BUROZEM_FARMLAND.get(),
                    ModBlocks.BUROZEM_GRASS.get(), ModBlocks.BUROZEM_FARMLAND.get(),
                    ModBlocks.CHERNOZEM.get(), ModBlocks.CHERNOZEM_FARMLAND.get(),
                    ModBlocks.CHERNOZEM_GRASS.get(), ModBlocks.CHERNOZEM_FARMLAND.get());
        }
        return bedBySoil.get(block);
    }

    /**
     * Тяпка (и любая мотыга) вспахивает жёлтозём, бурозём и чернозём — голые и с дёрном — в грядку своей почвы.
     * Как у ванильной земли: только сверху или сбоку и если над блоком воздух.
     */
    @SubscribeEvent
    public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.HOE_TILL) {
            return;
        }
        Block bed = bedFor(event.getState().getBlock());
        UseOnContext context = event.getContext();
        if (bed != null && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            event.setFinalState(bed.defaultBlockState());
        }
    }

    /**
     * На сухой грядке растения не растут. На медленной почве (жёлтозём) часть попыток роста растения отменяется.
     * Быструю почву ускоряет сама грядка.
     */
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        LevelAccessor level = event.getLevel();
        BlockState soil = level.getBlockState(event.getPos().below());
        if (soil.getBlock() instanceof GardenBedBlock bed && (GardenBedBlock.isDry(soil) || bed.growthSpeed() < 1
                && level.getRandom().nextFloat() >= bed.growthSpeed())) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    private FarmEvents() {}
}

package io.github.ChanesCMD.trailcraft.init;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.fluids.drain.ItemDrainBlock;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.spout.SpoutBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.pipe.ReinforcedFluidTankBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Укреплённые жидкостные детали Create (из прочных листов): помпа, умная труба, вентиль, ручка вентиля, шкив шланга,
 * носик, слив, интерфейс жидкостей, бак. Это сами блоки Create (те же блок-сущности, рендер и поведение), лава их не
 * прожигает — обычные медные прогорают (pipe/LavaPipes). Бак — свой класс и тип блок-сущности (pipe/ReinforcedFluidTankBlock).
 * Модели и текстуры (детали Create в тёмной гамме прочного листа), рецепты, переводы — scripts/gen_reinforced_parts.py.
 */
public final class ModReinforced {
    public static final List<DeferredBlock<? extends Block>> BLOCKS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();

    public static final DeferredBlock<PumpBlock> PUMP = block("reinforced_mechanical_pump", PumpBlock::new);
    public static final DeferredBlock<SmartFluidPipeBlock> SMART_PIPE = block("reinforced_smart_fluid_pipe", SmartFluidPipeBlock::new);
    public static final DeferredBlock<FluidValveBlock> VALVE = block("reinforced_fluid_valve", FluidValveBlock::new);
    /** Ручка вентиля сама жидкость не держит — та же ручка Create, чёрная (её рисует рендер Create по цвету). */
    public static final DeferredBlock<ValveHandleBlock> VALVE_HANDLE = block("reinforced_valve_handle",
            p -> ValveHandleBlock.dyed(p, DyeColor.BLACK));
    public static final DeferredBlock<HosePulleyBlock> HOSE_PULLEY = block("reinforced_hose_pulley", HosePulleyBlock::new);
    public static final DeferredBlock<SpoutBlock> SPOUT = block("reinforced_spout", SpoutBlock::new);
    public static final DeferredBlock<ItemDrainBlock> DRAIN = block("reinforced_item_drain", ItemDrainBlock::new);
    public static final DeferredBlock<PortableStorageInterfaceBlock> FLUID_INTERFACE = block("reinforced_portable_fluid_interface",
            PortableStorageInterfaceBlock::forFluids);
    public static final DeferredBlock<ReinforcedFluidTankBlock> TANK = ModBlocks.BLOCKS.registerBlock("reinforced_fluid_tank",
            ReinforcedFluidTankBlock::new, props().isRedstoneConductor((state, level, pos) -> true));

    static {
        BLOCKS.add(TANK);
        ITEMS.add(ModItems.ITEMS.registerItem("reinforced_fluid_tank", p -> new FluidTankItem(TANK.get(), p)));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> TANK_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("reinforced_fluid_tank", () -> {
                BlockEntityType<FluidTankBlockEntity>[] type = new BlockEntityType[1];
                type[0] = BlockEntityType.Builder.of((pos, state) -> new FluidTankBlockEntity(type[0], pos, state), TANK.get()).build(null);
                return type[0];
            });

    private ModReinforced() {}

    public static void init() {}

    /** Прочнее медных деталей и тёмные, как прочный лист; свет пропускают, как детали Create. */
    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 1200.0f)
                .sound(SoundType.NETHERITE_BLOCK).noOcclusion();
    }

    private static <B extends Block> DeferredBlock<B> block(String id, Function<BlockBehaviour.Properties, B> factory) {
        DeferredBlock<B> block = ModBlocks.BLOCKS.registerBlock(id, factory, props());
        BLOCKS.add(block);
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(id, block));
        return block;
    }

    /** Укреплённая ли деталь (её лава не прожигает). */
    public static boolean isReinforced(Block block) {
        for (DeferredBlock<? extends Block> b : BLOCKS) {
            if (b.get() == block) {
                return true;
            }
        }
        return false;
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        /** Блок-сущности Create — и для укреплённых блоков. */
        @SubscribeEvent
        public static void onAddBlocks(BlockEntityTypeAddBlocksEvent event) {
            event.modify(AllBlockEntityTypes.MECHANICAL_PUMP.get(), PUMP.get());
            event.modify(AllBlockEntityTypes.SMART_FLUID_PIPE.get(), SMART_PIPE.get());
            event.modify(AllBlockEntityTypes.FLUID_VALVE.get(), VALVE.get());
            event.modify(AllBlockEntityTypes.VALVE_HANDLE.get(), VALVE_HANDLE.get());
            event.modify(AllBlockEntityTypes.HOSE_PULLEY.get(), HOSE_PULLEY.get());
            event.modify(AllBlockEntityTypes.SPOUT.get(), SPOUT.get());
            event.modify(AllBlockEntityTypes.ITEM_DRAIN.get(), DRAIN.get());
            event.modify(AllBlockEntityTypes.PORTABLE_FLUID_INTERFACE.get(), FLUID_INTERFACE.get());
        }

        /** Нагрузка и мощность — как у таких же деталей Create (берутся из их настроек). */
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                BlockStressValues.IMPACTS.register(PUMP.get(), () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()));
                BlockStressValues.IMPACTS.register(VALVE.get(), () -> BlockStressValues.getImpact(AllBlocks.FLUID_VALVE.get()));
                BlockStressValues.IMPACTS.register(HOSE_PULLEY.get(), () -> BlockStressValues.getImpact(AllBlocks.HOSE_PULLEY.get()));
                BlockStressValues.CAPACITIES.register(VALVE_HANDLE.get(),
                        () -> BlockStressValues.getCapacity(AllBlocks.COPPER_VALVE_HANDLE.get()));
            });
        }

        /** Бак отдаёт трубам жидкость своего многоблочного бака (бак-контроллер). */
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TANK_ENTITY.get(), (tank, side) -> {
                FluidTankBlockEntity controller = tank.getControllerBE();
                return controller == null ? null : controller.getTankInventory();
            });
        }
    }
}

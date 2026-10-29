package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.block.entity.WaterBarrelBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.CoalBurnerBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.WoodChestBlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.entity.BackpackBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.GraveBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.TrailCampfireBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TrailCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>> BACKPACK = BLOCK_ENTITIES.register("backpack",
            () -> BlockEntityType.Builder.of(BackpackBlockEntity::new,
                    ModBlocks.DUFFEL_BACKPACK.get(), ModBlocks.LEATHER_BACKPACK.get(), ModBlocks.TACTICAL_BACKPACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE = BLOCK_ENTITIES.register("grave",
            () -> BlockEntityType.Builder.of(GraveBlockEntity::new, ModBlocks.GRAVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrailCampfireBlockEntity>> CAMPFIRE = BLOCK_ENTITIES.register("campfire",
            () -> BlockEntityType.Builder.of(TrailCampfireBlockEntity::new, ModBlocks.CAMPFIRE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterBarrelBlockEntity>> WATER_BARREL = BLOCK_ENTITIES.register("water_barrel",
            () -> BlockEntityType.Builder.of(WaterBarrelBlockEntity::new, ModBlocks.WATER_BARREL.get()).build(null));

    /** Сундуки по породам (ModChests): один тип на все. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodChestBlockEntity>> WOOD_CHEST = BLOCK_ENTITIES.register("wood_chest",
            () -> BlockEntityType.Builder.of(WoodChestBlockEntity::new,
                    ModChests.CHESTS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalBurnerBlockEntity>> COAL_BURNER = BLOCK_ENTITIES.register("coal_burner",
            () -> BlockEntityType.Builder.of(CoalBurnerBlockEntity::new, ModBlocks.COAL_BURNER.get()).build(null));

    /** Бочка отдаёт свой бак трубам и помпам Create (FluidHandler), горелка принимает топливо (ItemHandler). */
    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Capabilities {
        @SubscribeEvent
        public static void onRegister(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, WATER_BARREL.get(),
                    (barrel, side) -> barrel.tank);
            // топливо в угольную горелку — воронками и желобами Create
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, COAL_BURNER.get(),
                    (burner, side) -> burner.fuelSlot);
        }
    }

    /** Укреплённая труба — с блок-сущностью трубы Create (FluidPipeBlockEntity), её тип должен знать наш блок. */
    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class CreatePipes {
        @SubscribeEvent
        public static void onAddBlocks(net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
            event.modify(com.simibubi.create.AllBlockEntityTypes.FLUID_PIPE.get(), ModBlocks.REINFORCED_FLUID_PIPE.get());
        }
    }

    private ModBlockEntities() {}
}

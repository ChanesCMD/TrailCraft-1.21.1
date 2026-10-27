package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.entity.BackpackBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.GraveBlockEntity;
import io.github.ChanesCMD.trailcraft.block.entity.NuggetMarkerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
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

    /** ВРЕМЕННАЯ: столб над самородками для проверки генерации (block/NuggetBlock). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuggetMarkerBlockEntity>> NUGGET_MARKER =
            BLOCK_ENTITIES.register("nugget_marker", () -> BlockEntityType.Builder.of(NuggetMarkerBlockEntity::new,
                    ModOres.NUGGET_BLOCKS.values().stream().map(b -> b.get()).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}

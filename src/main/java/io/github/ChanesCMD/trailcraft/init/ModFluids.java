package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Свои жидкости (смеситель Create их делает, трубы качают, вёдрами разливаются): креозот — для пропитанных палок
 * (наливатель), танин — для дубления шкур в бочке. Текстура — ванильная вода, перекрашенная в цвет жидкости
 * (ClientEvents.clientExtensions). JSON, текстуры вёдер и переводы — scripts/gen_fluids.py.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TrailCraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TrailCraft.MODID);
    public static final List<Liquid> ALL = new ArrayList<>();

    public static final Liquid CREOSOTE = new Liquid("creosote", 0xFF2A1C10, 1100, 1500);
    public static final Liquid TANNIN = new Liquid("tannin", 0xFF7A4A22, 1000, 1000);

    private ModFluids() {}

    public static void init() {}

    /** Жидкость: тип, источник и течение, блок в мире и ведро. color — ARGB, им красится текстура воды. */
    public static final class Liquid {
        public final String id;
        public final int color;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<LiquidBlock> block;
        public final DeferredItem<BucketItem> bucket;
        private BaseFlowingFluid.Properties properties;

        Liquid(String id, int color, int density, int viscosity) {
            this.id = id;
            this.color = color;
            type = FLUID_TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + TrailCraft.MODID + "." + id).density(density).viscosity(viscosity)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
            source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties));
            flowing = FLUIDS.register("flowing_" + id, () -> new BaseFlowingFluid.Flowing(properties));
            block = ModBlocks.BLOCKS.register(id, () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
            bucket = ModItems.ITEMS.register(id + "_bucket",
                    () -> new BucketItem(source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            properties = new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket);
            ALL.add(this);
        }
    }
}

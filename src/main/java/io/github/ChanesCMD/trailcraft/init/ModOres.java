package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.block.OreBlock;
import io.github.ChanesCMD.trailcraft.block.PebbleBlock;
import io.github.ChanesCMD.trailcraft.item.OreBlockItem;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Руды (список — {@link Ore}), то, что с них падает, самородки на поверхности, слитки, шихта и древесный уголь.
 * Медь, железо, золото — ванильные слитки, цинк — слиток Create (create:zinc_ingot).
 * Плавятся на костре (рецепты campfire_cooking), шихта бронз дробится из кусков руды молотком на верстаке.
 */
public final class ModOres {
    public static final Map<Ore, DeferredBlock<OreBlock>> BLOCKS = new EnumMap<>(Ore.class);
    public static final Map<Ore, DeferredItem<OreBlockItem>> BLOCK_ITEMS = new EnumMap<>(Ore.class);
    /** Что падает с руды: кусок raw_*, у угля — каменный уголь, у апатита — апатит, у кварцевой жилы — ванильный кварц. */
    public static final Map<Ore, DeferredItem<Item>> DROPS = new EnumMap<>(Ore.class);

    public static final DeferredItem<Item> CHARCOAL = ModItems.ITEMS.registerSimpleItem("charcoal");

    // === СЛИТКИ ===
    public static final DeferredItem<Item> TIN_INGOT = ModItems.ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = ModItems.ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> BISMUTH_INGOT = ModItems.ITEMS.registerSimpleItem("bismuth_ingot");
    public static final DeferredItem<Item> NICKEL_INGOT = ModItems.ITEMS.registerSimpleItem("nickel_ingot");
    public static final DeferredItem<Item> LITHIUM_INGOT = ModItems.ITEMS.registerSimpleItem("lithium_ingot");
    public static final DeferredItem<Item> ALUMINUM_INGOT = ModItems.ITEMS.registerSimpleItem("aluminum_ingot");
    public static final DeferredItem<Item> TIN_BRONZE_INGOT = ModItems.ITEMS.registerSimpleItem("tin_bronze_ingot");
    public static final DeferredItem<Item> BISMUTH_BRONZE_INGOT = ModItems.ITEMS.registerSimpleItem("bismuth_bronze_ingot");
    public static final List<DeferredItem<Item>> INGOTS = List.of(TIN_INGOT, LEAD_INGOT, BISMUTH_INGOT, NICKEL_INGOT,
            LITHIUM_INGOT, ALUMINUM_INGOT, TIN_BRONZE_INGOT, BISMUTH_BRONZE_INGOT);

    // === ШИХТА БРОНЗ: дробится молотком из кусков руды на верстаке, плавится на костре в слиток бронзы ===
    public static final DeferredItem<Item> TIN_BRONZE_MIX = ModItems.ITEMS.registerSimpleItem("tin_bronze_mix");
    public static final DeferredItem<Item> BISMUTH_BRONZE_MIX = ModItems.ITEMS.registerSimpleItem("bismuth_bronze_mix");
    /** Латунь: медь и цинк, плавится в слиток латуни Create. */
    public static final DeferredItem<Item> BRASS_MIX = ModItems.ITEMS.registerSimpleItem("brass_mix");
    // Чистая шихта: самородки, раздробленные молотком; смешивается в сплавы уже без молотка
    public static final DeferredItem<Item> COPPER_MIX = ModItems.ITEMS.registerSimpleItem("copper_mix");
    public static final DeferredItem<Item> TIN_MIX = ModItems.ITEMS.registerSimpleItem("tin_mix");
    public static final DeferredItem<Item> ZINC_MIX = ModItems.ITEMS.registerSimpleItem("zinc_mix");
    public static final List<DeferredItem<Item>> MIXES = List.of(COPPER_MIX, TIN_MIX, ZINC_MIX, TIN_BRONZE_MIX,
            BISMUTH_BRONZE_MIX, BRASS_MIX);

    /**
     * Самородки: лежат на поверхности над большой жилой своей руды (worldgen/NuggetDepositFeature), подбираются ПКМ,
     * как кремень. Блок и предмет — {@code <руда>_nugget}.
     */
    public static final List<Ore> NUGGET_ORES = List.of(Ore.NATIVE_COPPER, Ore.MALACHITE, Ore.TETRAHEDRITE,
            Ore.CASSITERITE, Ore.SPHALERITE);
    public static final Map<Ore, DeferredBlock<Block>> NUGGET_BLOCKS = new EnumMap<>(Ore.class);
    public static final Map<Ore, DeferredItem<BlockItem>> NUGGETS = new EnumMap<>(Ore.class);

    static {
        for (Ore ore : Ore.values()) {
            DeferredBlock<OreBlock> block = ModBlocks.BLOCKS.registerBlock(ore.blockId(), p -> OreBlock.create(ore, p),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
            BLOCKS.put(ore, block);
            BLOCK_ITEMS.put(ore, ModItems.ITEMS.registerItem(ore.blockId(), p -> new OreBlockItem(block.get(), p)));
            DROPS.put(ore, ore.dropId.contains(":") ? DeferredItem.createItem(ResourceLocation.parse(ore.dropId))
                    : ModItems.ITEMS.registerSimpleItem(ore.dropId));
        }
        for (Ore ore : NUGGET_ORES) {
            DeferredBlock<Block> block = ModBlocks.BLOCKS.registerBlock(ore.id + "_nugget", PebbleBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.2f).sound(SoundType.STONE)
                            .noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));
            NUGGET_BLOCKS.put(ore, block);
            NUGGETS.put(ore, ModItems.ITEMS.registerSimpleBlockItem(ore.id + "_nugget", block));
        }
    }

    /** Вызывается из конструктора мода, чтобы статический блок выше выполнился до событий регистрации. */
    public static void init() {}

    private ModOres() {}
}

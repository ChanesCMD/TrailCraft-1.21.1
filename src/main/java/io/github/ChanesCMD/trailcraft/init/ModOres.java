package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.block.OreBlock;
import io.github.ChanesCMD.trailcraft.item.OreBlockItem;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Руды (список — {@link Ore}), то, что с них падает, слитки, шихта бронз и древесный уголь.
 * Медь, железо, золото — ванильные слитки, цинк — слиток Create (create:zinc_ingot).
 * Плавятся на костре (рецепты campfire_cooking), шихта бронз смешивается на верстаке.
 */
public final class ModOres {
    public static final Map<Ore, DeferredBlock<OreBlock>> BLOCKS = new EnumMap<>(Ore.class);
    public static final Map<Ore, DeferredItem<OreBlockItem>> BLOCK_ITEMS = new EnumMap<>(Ore.class);
    /** Что падает с руды: кусок raw_*, у угля — каменный уголь, у апатита — апатит. */
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

    // === ШИХТА БРОНЗ: смешивается из слитков на верстаке, плавится на костре в слиток бронзы ===
    public static final DeferredItem<Item> TIN_BRONZE_MIX = ModItems.ITEMS.registerSimpleItem("tin_bronze_mix");
    public static final DeferredItem<Item> BISMUTH_BRONZE_MIX = ModItems.ITEMS.registerSimpleItem("bismuth_bronze_mix");

    static {
        for (Ore ore : Ore.values()) {
            DeferredBlock<OreBlock> block = ModBlocks.BLOCKS.registerBlock(ore.blockId(), p -> OreBlock.create(ore, p),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));
            BLOCKS.put(ore, block);
            BLOCK_ITEMS.put(ore, ModItems.ITEMS.registerItem(ore.blockId(), p -> new OreBlockItem(block.get(), p)));
            DROPS.put(ore, ModItems.ITEMS.registerSimpleItem(ore.dropId));
        }
    }

    /** Вызывается из конструктора мода, чтобы статический блок выше выполнился до событий регистрации. */
    public static void init() {}

    private ModOres() {}
}

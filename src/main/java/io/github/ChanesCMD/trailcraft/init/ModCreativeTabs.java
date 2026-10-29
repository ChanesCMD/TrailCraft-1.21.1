package io.github.ChanesCMD.trailcraft.init;

import com.simibubi.create.AllItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import io.github.ChanesCMD.trailcraft.ore.Rock;

/** Вкладки по порядку: общая, деревья, руды, инструменты, растительная еда, животная еда, яйца призыва. */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrailCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft"))
                    .icon(() -> ModBlocks.ANDESITE.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Камни (12)
                        output.accept(ModBlocks.ANDESITE);
                        output.accept(ModBlocks.BASALT);
                        output.accept(ModBlocks.CHALK);
                        output.accept(ModBlocks.DIORITE);
                        output.accept(ModBlocks.DOLOMITE);
                        output.accept(ModBlocks.GABBRO);
                        output.accept(ModBlocks.GRANITE);
                        output.accept(ModBlocks.GRAY_GRANITE);
                        output.accept(ModBlocks.LIMESTONE);
                        output.accept(ModBlocks.MARBLE);
                        output.accept(ModBlocks.SANDSTONE);
                        output.accept(ModBlocks.SHALE);

                        // Базовые блоки (9)
                        output.accept(ModBlocks.ZHELTOZEM);
                        output.accept(ModBlocks.BUROZEM);
                        output.accept(ModBlocks.CHERNOZEM);
                        output.accept(ModBlocks.LOAM);
                        output.accept(ModBlocks.CLAY_WHITE);
                        output.accept(ModBlocks.CLAY_BLUE);
                        output.accept(ModBlocks.CLAY_RED);
                        output.accept(ModBlocks.CLAY_GRAY);
                        output.accept(ModBlocks.PEAT);

                        // Травяные пары (9)
                        output.accept(ModBlocks.ZHELTOZEM_GRASS);
                        output.accept(ModBlocks.BUROZEM_GRASS);
                        output.accept(ModBlocks.CHERNOZEM_GRASS);
                        output.accept(ModBlocks.LOAM_GRASS);
                        output.accept(ModBlocks.CLAY_WHITE_GRASS);
                        output.accept(ModBlocks.CLAY_BLUE_GRASS);
                        output.accept(ModBlocks.CLAY_RED_GRASS);
                        output.accept(ModBlocks.CLAY_GRAY_GRASS);
                        output.accept(ModBlocks.PEAT_GRASS);

                        // Грядки (3)
                        output.accept(ModItems.ZHELTOZEM_FARMLAND);
                        output.accept(ModItems.BUROZEM_FARMLAND);
                        output.accept(ModItems.CHERNOZEM_FARMLAND);

                        // Гравий (12)
                        output.accept(ModBlocks.ANDESITE_GRAVEL);
                        output.accept(ModBlocks.BASALT_GRAVEL);
                        output.accept(ModBlocks.CHALK_GRAVEL);
                        output.accept(ModBlocks.DIORITE_GRAVEL);
                        output.accept(ModBlocks.DOLOMITE_GRAVEL);
                        output.accept(ModBlocks.GABBRO_GRAVEL);
                        output.accept(ModBlocks.GRANITE_GRAVEL);
                        output.accept(ModBlocks.GRAY_GRANITE_GRAVEL);
                        output.accept(ModBlocks.LIMESTONE_GRAVEL);
                        output.accept(ModBlocks.MARBLE_GRAVEL);
                        output.accept(ModBlocks.SANDSTONE_GRAVEL);
                        output.accept(ModBlocks.SHALE_GRAVEL);
                        ModBlocks.COBBLESTONES.values().forEach(output::accept);
                        ModBlocks.SLABS.values().forEach(output::accept);

                        // Камешки (12)
                        output.accept(ModBlocks.ANDESITE_PEBBLE);
                        output.accept(ModBlocks.BASALT_PEBBLE);
                        output.accept(ModBlocks.CHALK_PEBBLE);
                        output.accept(ModBlocks.DIORITE_PEBBLE);
                        output.accept(ModBlocks.DOLOMITE_PEBBLE);
                        output.accept(ModBlocks.GABBRO_PEBBLE);
                        output.accept(ModBlocks.GRANITE_PEBBLE);
                        output.accept(ModBlocks.GRAY_GRANITE_PEBBLE);
                        output.accept(ModBlocks.LIMESTONE_PEBBLE);
                        output.accept(ModBlocks.MARBLE_PEBBLE);
                        output.accept(ModBlocks.SANDSTONE_PEBBLE);
                        output.accept(ModBlocks.SHALE_PEBBLE);
                        output.accept(ModItems.FLINT);
                        output.accept(ModItems.CHOPPING_STUMP);
                        output.accept(ModItems.RAW_HIDE);
                        output.accept(ModItems.SCRAPED_HIDE);
                        output.accept(ModItems.LEATHER);
                        output.accept(ModItems.SEAWEED);
                        output.accept(ModItems.DRIED_SEAWEED_BLOCK);
                        output.accept(ModItems.CACTUS);
                        output.accept(ModItems.GREEN_DYE);
                        output.accept(ModItems.WHEAT_SEEDS);
                        output.accept(ModItems.WHEAT);
                        output.accept(ModItems.SUGAR_CANE);
                        output.accept(ModItems.SUGAR_BEET_SEEDS);
                        output.accept(ModItems.COCOA_BEANS);
                        output.accept(ModItems.BITUMINOUS_COAL_BLOCK);
                        output.accept(ModItems.COAL_BURNER);
                        output.accept(ModItems.REINFORCED_FLUID_PIPE);
                        ModBees.HIVE_ITEMS.values().forEach(output::accept);
                        ModBees.BEES.values().forEach(output::accept);
                        ModBees.COMB_ITEMS.forEach(output::accept);
                        output.accept(ModBees.BEESWAX);
                        output.accept(ModBees.APIARY_ITEM);
                        output.accept(ModItems.WATER_BARREL);
                        ModChests.BARREL_ITEMS.values().forEach(output::accept);
                        ModChests.ITEMS.values().forEach(output::accept);
                        output.accept(ModItems.STRAW_BLOCK);
                        output.accept(ModItems.STRAW_BED);
                        output.accept(ModItems.WOODEN_SUPPORT);
                        output.accept(ModItems.IRON_SUPPORT);
                        output.accept(ModItems.DUFFEL_BACKPACK);
                        output.accept(ModItems.LEATHER_BACKPACK);
                        output.accept(ModItems.TACTICAL_BACKPACK);
                        output.accept(ModItems.SMALL_POCKET);
                        output.accept(ModItems.MEDIUM_POCKET);
                        output.accept(ModItems.LARGE_POCKET);
                        output.accept(ModItems.BELT);
                        output.accept(ModItems.FLASK);
                        output.accept(ModItems.WALLET);
                        output.accept(ModItems.BRASS_ALTIMETER);
                        output.accept(ModItems.SEXTANT);
                        output.accept(ModItems.CHRONOMETER);
                        output.accept(ModItems.MINI_PRECISION_MECHANISM);
                        output.accept(ModItems.GPS);
                        ModItems.COINS.forEach(output::accept);
                        output.accept(ModItems.CALENDAR);
                        output.accept(ModItems.ATLAS);

                        // Песок (4)
                        output.accept(ModBlocks.SAND_WHITE);
                        output.accept(ModBlocks.SAND_YELLOW);
                        output.accept(ModBlocks.SAND_GRAY);
                        output.accept(ModBlocks.SAND_RED);

                        // Ил (1)
                        output.accept(ModBlocks.SILT);

                        // Трава (3)
                        output.accept(ModBlocks.GRASS_MEDIUM);
                        output.accept(ModBlocks.GRASS_SMALL);
                        output.accept(ModBlocks.GRASS_TALL);

                        // Дикие растения, джут
                        output.accept(ModItems.CURRANT_BUSH);
                        output.accept(ModItems.CLOUDBERRY_BUSH);
                        output.accept(ModItems.GOOSEBERRY_BUSH);
                        output.accept(ModItems.BLACKBERRY_BUSH);
                        output.accept(ModItems.WILD_CARROT);
                        output.accept(ModItems.WILD_POTATO);
                        output.accept(ModItems.WILD_JUTE);
                        output.accept(ModItems.JUTE);
                        output.accept(ModItems.JUTE_SEEDS);
                        output.accept(ModItems.ROPE);

                        // Предметы
                        output.accept(ModItems.CLAY_BALL_WHITE);
                        output.accept(ModItems.CLAY_BALL_BLUE);
                        output.accept(ModItems.CLAY_BALL_GRAY);
                        output.accept(ModItems.CLAY_BALL_RED);

                        // Палки
                        output.accept(ModItems.STICK_BRANCH);
                        output.accept(ModItems.STICK_PROCESSED);
                        output.accept(ModItems.STICK_IMPREGNATED);
                        output.accept(ModItems.LONG_STICK);
                        output.accept(ModItems.CAMPFIRE);
                        output.accept(ModItems.ASH);
                        output.accept(ModItems.BARK);
                        output.accept(ModItems.SAWDUST);
                        output.accept(ModItems.PAPER_BLANK);
                        output.accept(ModItems.CHARCOAL_DUST);
                        ModFluids.ALL.forEach(liquid -> output.accept(liquid.bucket));
                        output.accept(ModItems.RESIN);
                        output.accept(ModItems.JUTE_THREAD);

                        // Глина, кирпичи и плавка (ModSmelting)
                        for (var brick : ModSmelting.BRICKS.values()) {
                            output.accept(brick);
                        }
                        for (var bricks : ModSmelting.BRICK_BLOCK_ITEMS.values()) {
                            output.accept(bricks);
                        }
                        output.accept(ModItems.CLOTH_BLANK);
                        output.accept(ModItems.CLOTH);
                        output.accept(ModSmelting.STRAW);
                        output.accept(ModSmelting.UNFIRED_REINFORCED_BRICK);
                        output.accept(ModSmelting.REINFORCED_BRICK);
                        output.accept(ModSmelting.SMALL_BLOOMERY_ITEM);
                        output.accept(ModSmelting.CLAY_MOLD);
                        output.accept(ModSmelting.UNFIRED_JUG);
                        output.accept(ModSmelting.JUG);
                        // Формы для литья (сырые и обожжённые) и головки инструментов из них
                        ModSmelting.UNFIRED_MOLDS.values().forEach(output::accept);
                        ModSmelting.MOLDS.values().forEach(output::accept);
                        ModSmelting.HEADS.values().forEach(output::accept);
                        output.accept(ModItems.FEATHER);
                        output.accept(ModItems.BONE);

                        // Мусор
                        output.accept(ModItems.METAL_SCRAP);
                        output.accept(ModItems.PAPER_SCRAPS);
                        output.accept(ModItems.PLASTIC_WASTE);
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_WOOD_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_wood_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_wood"))
                    .withTabsBefore(TRAILCRAFT_TAB.getId())
                    .icon(() -> ModWood.SETS.get(0).log.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (ModWood.WoodSet set : ModWood.SETS) {
                            for (var item : set.items) {
                                output.accept(item);
                            }
                        }
                    }).build());

    /** Руды (каждая во всех своих породах), то, что с них падает, уголь, шихта, порошок гематита и слитки. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_ORES_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_ores_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_ores"))
                    .withTabsBefore(TRAILCRAFT_WOOD_TAB.getId())
                    .icon(() -> ModOres.BLOCK_ITEMS.get(Ore.MALACHITE).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (Ore ore : Ore.values()) {
                            for (Rock rock : ore.rocks) {
                                output.accept(ModOres.BLOCKS.get(ore).get().stack(rock));
                            }
                        }
                        for (Ore ore : Ore.values()) {
                            output.accept(ModOres.DROPS.get(ore));
                        }
                        output.accept(ModOres.CHARCOAL);
                        ModOres.NUGGET_ORES.forEach(ore -> output.accept(ModOres.NUGGETS.get(ore)));
                        ModOres.MIXES.forEach(output::accept);
                        output.accept(ModItems.HEMATITE_POWDER);
                        // слитки меди, железа и золота — ванильные, цинка и латуни — Create
                        output.accept(Items.COPPER_INGOT);
                        output.accept(Items.IRON_INGOT);
                        output.accept(Items.GOLD_INGOT);
                        output.accept(AllItems.ZINC_INGOT.get());
                        output.accept(AllItems.BRASS_INGOT.get());
                        ModOres.INGOTS.forEach(output::accept);
                    }).build());

    /** Инструменты и оружие: ModTools (по виду, внутри — по материалу), молотки, дёрнорез, огниво. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_tools_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_tools"))
                    .withTabsBefore(TRAILCRAFT_ORES_TAB.getId())
                    .icon(() -> ModTools.ITEMS.get(0).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.STONE_HAMMER);
                        output.accept(ModItems.STEIGER_HAMMER);
                        output.accept(ModItems.GEOLOGICAL_HAMMER);
                        output.accept(ModItems.TURF_CUTTER);
                        output.accept(ModItems.FIRESTARTER);
                        for (var item : ModTools.ITEMS) {
                            output.accept(item);
                        }
                    }).build());

    /** Растительная еда: плоды, ягоды, овощи, хлеб. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLANT_FOOD_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_plant_food_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_plant_food"))
                    .withTabsBefore(TOOLS_TAB.getId())
                    .icon(() -> ModItems.APPLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.APPLE);
                        output.accept(ModItems.PEAR);
                        output.accept(ModItems.PLUM);
                        output.accept(ModItems.CHERRY);
                        output.accept(ModItems.APRICOT);
                        output.accept(ModItems.CURRANT);
                        output.accept(ModItems.CLOUDBERRY);
                        output.accept(ModItems.GOOSEBERRY);
                        output.accept(ModItems.BLACKBERRY);
                        output.accept(Items.CARROT);
                        output.accept(Items.POTATO);
                        output.accept(Items.BAKED_POTATO);
                        output.accept(ModItems.SUGAR_BEET);
                        output.accept(ModItems.DRIED_SEAWEED);
                        output.accept(Items.BREAD);
                    }).build());

    /** Животная еда: мясо сырое и жареное (курятина — «белое мясо»), рыба, яйцо, мёд. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANIMAL_FOOD_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_animal_food_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_animal_food"))
                    .withTabsBefore(PLANT_FOOD_TAB.getId())
                    .icon(() -> Items.COOKED_BEEF.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (Item item : new Item[] {Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
                                Items.MUTTON, Items.COOKED_MUTTON, Items.CHICKEN, Items.COOKED_CHICKEN, Items.RABBIT,
                                Items.COOKED_RABBIT, Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON}) {
                            output.accept(item);
                        }
                        output.accept(ModItems.EGG);
                        output.accept(Items.HONEY_BOTTLE);
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SPAWN_EGGS_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_spawn_eggs_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_spawn_eggs"))
                    .withTabsBefore(ANIMAL_FOOD_TAB.getId())
                    .icon(() -> ModItems.CAPERCAILLIE_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CAPERCAILLIE_SPAWN_EGG);
                        ModItems.SPAWN_EGGS.values().forEach(output::accept);
                    }).build());
}

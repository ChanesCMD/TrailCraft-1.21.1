package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import io.github.ChanesCMD.trailcraft.ore.Rock;

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
                        output.accept(ModItems.DUFFEL_BACKPACK);
                        output.accept(ModItems.LEATHER_BACKPACK);
                        output.accept(ModItems.TACTICAL_BACKPACK);
                        output.accept(ModItems.SMALL_POCKET);
                        output.accept(ModItems.MEDIUM_POCKET);
                        output.accept(ModItems.LARGE_POCKET);
                        output.accept(ModItems.BELT);
                        output.accept(ModItems.FLASK);
                        output.accept(ModItems.WALLET);
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

                        // Дикие растения, ягоды, джут
                        output.accept(ModItems.CURRANT_BUSH);
                        output.accept(ModItems.CLOUDBERRY_BUSH);
                        output.accept(ModItems.GOOSEBERRY_BUSH);
                        output.accept(ModItems.BLACKBERRY_BUSH);
                        output.accept(ModItems.CURRANT);
                        output.accept(ModItems.CLOUDBERRY);
                        output.accept(ModItems.GOOSEBERRY);
                        output.accept(ModItems.BLACKBERRY);
                        output.accept(ModItems.WILD_CARROT);
                        output.accept(ModItems.WILD_POTATO);
                        output.accept(ModItems.WILD_JUTE);
                        output.accept(ModItems.JUTE);
                        output.accept(ModItems.JUTE_SEEDS);
                        output.accept(ModItems.ROPE);

                        // Предметы
                        output.accept(ModItems.TURF_CUTTER);
                        output.accept(ModItems.CLAY_BALL_WHITE);
                        output.accept(ModItems.CLAY_BALL_BLUE);
                        output.accept(ModItems.CLAY_BALL_GRAY);
                        output.accept(ModItems.CLAY_BALL_RED);

                        // Палки
                        output.accept(ModItems.STICK_BRANCH);
                        output.accept(ModItems.STICK_PROCESSED);
                        output.accept(ModItems.FIRESTARTER);
                        output.accept(ModItems.STONE_HAMMER);

                        // Инструменты и предметы (ModTools)
                        for (var item : ModTools.ITEMS) {
                            output.accept(item);
                        }

                        // Мусор
                        output.accept(ModItems.METAL_SCRAP);
                        output.accept(ModItems.PAPER_SCRAPS);
                        output.accept(ModItems.PLASTIC_WASTE);
                    }).build());

    /** Руды (каждая во всех своих породах), то, что с них падает, уголь, шихта и слитки. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_ORES_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_ores_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_ores"))
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
                        output.accept(ModOres.TIN_BRONZE_MIX);
                        output.accept(ModOres.BISMUTH_BRONZE_MIX);
                        ModOres.INGOTS.forEach(output::accept);
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_WOOD_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_wood_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft_wood"))
                    .icon(() -> ModWood.SETS.get(0).log.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (ModWood.WoodSet set : ModWood.SETS) {
                            for (var item : set.items) {
                                output.accept(item);
                            }
                        }
                    }).build());
}
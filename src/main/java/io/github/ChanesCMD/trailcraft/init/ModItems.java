package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import io.github.ChanesCMD.trailcraft.item.BranchStickItem;
import io.github.ChanesCMD.trailcraft.item.TurfCutterItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TrailCraft.MODID);

    // === КАМНИ (12) ===
    public static final DeferredItem<BlockItem> ANDESITE = ITEMS.registerSimpleBlockItem("andesite", ModBlocks.ANDESITE);
    public static final DeferredItem<BlockItem> BASALT = ITEMS.registerSimpleBlockItem("basalt", ModBlocks.BASALT);
    public static final DeferredItem<BlockItem> CHALK = ITEMS.registerSimpleBlockItem("chalk", ModBlocks.CHALK);
    public static final DeferredItem<BlockItem> DIORITE = ITEMS.registerSimpleBlockItem("diorite", ModBlocks.DIORITE);
    public static final DeferredItem<BlockItem> DOLOMITE = ITEMS.registerSimpleBlockItem("dolomite", ModBlocks.DOLOMITE);
    public static final DeferredItem<BlockItem> GABBRO = ITEMS.registerSimpleBlockItem("gabbro", ModBlocks.GABBRO);
    public static final DeferredItem<BlockItem> GRANITE = ITEMS.registerSimpleBlockItem("granite", ModBlocks.GRANITE);
    public static final DeferredItem<BlockItem> GRAY_GRANITE = ITEMS.registerSimpleBlockItem("gray_granite", ModBlocks.GRAY_GRANITE);
    public static final DeferredItem<BlockItem> LIMESTONE = ITEMS.registerSimpleBlockItem("limestone", ModBlocks.LIMESTONE);
    public static final DeferredItem<BlockItem> MARBLE = ITEMS.registerSimpleBlockItem("marble", ModBlocks.MARBLE);
    public static final DeferredItem<BlockItem> SANDSTONE = ITEMS.registerSimpleBlockItem("sandstone", ModBlocks.SANDSTONE);
    public static final DeferredItem<BlockItem> SHALE = ITEMS.registerSimpleBlockItem("shale", ModBlocks.SHALE);

    // === БАЗОВЫЕ БЛОКИ (9) ===
    public static final DeferredItem<BlockItem> ZHELTOZEM = ITEMS.registerSimpleBlockItem("zheltozem", ModBlocks.ZHELTOZEM);
    public static final DeferredItem<BlockItem> BUROZEM = ITEMS.registerSimpleBlockItem("burozem", ModBlocks.BUROZEM);
    public static final DeferredItem<BlockItem> CHERNOZEM = ITEMS.registerSimpleBlockItem("chernozem", ModBlocks.CHERNOZEM);
    public static final DeferredItem<BlockItem> LOAM = ITEMS.registerSimpleBlockItem("loam", ModBlocks.LOAM);
    public static final DeferredItem<BlockItem> CLAY_WHITE = ITEMS.registerSimpleBlockItem("clay_white", ModBlocks.CLAY_WHITE);
    public static final DeferredItem<BlockItem> CLAY_BLUE = ITEMS.registerSimpleBlockItem("clay_blue", ModBlocks.CLAY_BLUE);
    public static final DeferredItem<BlockItem> CLAY_RED = ITEMS.registerSimpleBlockItem("clay_red", ModBlocks.CLAY_RED);
    public static final DeferredItem<BlockItem> CLAY_GRAY = ITEMS.registerSimpleBlockItem("clay_gray", ModBlocks.CLAY_GRAY);
    public static final DeferredItem<BlockItem> PEAT = ITEMS.registerSimpleBlockItem("peat", ModBlocks.PEAT);

    // === ТРАВЯНЫЕ ПАРЫ (9) ===
    public static final DeferredItem<BlockItem> ZHELTOZEM_GRASS = ITEMS.registerSimpleBlockItem("zheltozem_grass", ModBlocks.ZHELTOZEM_GRASS);
    public static final DeferredItem<BlockItem> BUROZEM_GRASS = ITEMS.registerSimpleBlockItem("burozem_grass", ModBlocks.BUROZEM_GRASS);
    public static final DeferredItem<BlockItem> CHERNOZEM_GRASS = ITEMS.registerSimpleBlockItem("chernozem_grass", ModBlocks.CHERNOZEM_GRASS);
    public static final DeferredItem<BlockItem> LOAM_GRASS = ITEMS.registerSimpleBlockItem("loam_grass", ModBlocks.LOAM_GRASS);
    public static final DeferredItem<BlockItem> CLAY_WHITE_GRASS = ITEMS.registerSimpleBlockItem("clay_white_grass", ModBlocks.CLAY_WHITE_GRASS);
    public static final DeferredItem<BlockItem> CLAY_BLUE_GRASS = ITEMS.registerSimpleBlockItem("clay_blue_grass", ModBlocks.CLAY_BLUE_GRASS);
    public static final DeferredItem<BlockItem> CLAY_RED_GRASS = ITEMS.registerSimpleBlockItem("clay_red_grass", ModBlocks.CLAY_RED_GRASS);
    public static final DeferredItem<BlockItem> CLAY_GRAY_GRASS = ITEMS.registerSimpleBlockItem("clay_gray_grass", ModBlocks.CLAY_GRAY_GRASS);
    public static final DeferredItem<BlockItem> PEAT_GRASS = ITEMS.registerSimpleBlockItem("peat_grass", ModBlocks.PEAT_GRASS);

    // === ПЕСОК (4) ===
    public static final DeferredItem<BlockItem> SAND_WHITE = ITEMS.registerSimpleBlockItem("sand_white", ModBlocks.SAND_WHITE);
    public static final DeferredItem<BlockItem> SAND_YELLOW = ITEMS.registerSimpleBlockItem("sand_yellow", ModBlocks.SAND_YELLOW);
    public static final DeferredItem<BlockItem> SAND_GRAY = ITEMS.registerSimpleBlockItem("sand_gray", ModBlocks.SAND_GRAY);
    public static final DeferredItem<BlockItem> SAND_RED = ITEMS.registerSimpleBlockItem("sand_red", ModBlocks.SAND_RED);

    // === ИЛ (1) ===
    public static final DeferredItem<BlockItem> SILT = ITEMS.registerSimpleBlockItem("silt", ModBlocks.SILT);

    // === ТРАВА (3) ===
    public static final DeferredItem<BlockItem> GRASS_MEDIUM = ITEMS.registerSimpleBlockItem("grass_medium", ModBlocks.GRASS_MEDIUM);
    public static final DeferredItem<BlockItem> GRASS_SMALL = ITEMS.registerSimpleBlockItem("grass_small", ModBlocks.GRASS_SMALL);
    public static final DeferredItem<BlockItem> GRASS_TALL = ITEMS.registerSimpleBlockItem("grass_tall", ModBlocks.GRASS_TALL);

    // === ПРЕДМЕТЫ ===
    public static final DeferredItem<Item> CLAY_BALL_WHITE = ITEMS.registerSimpleItem("clay_ball_white");
    public static final DeferredItem<Item> CLAY_BALL_BLUE = ITEMS.registerSimpleItem("clay_ball_blue");
    public static final DeferredItem<Item> CLAY_BALL_GRAY = ITEMS.registerSimpleItem("clay_ball_gray");
    public static final DeferredItem<Item> CLAY_BALL_RED = ITEMS.registerSimpleItem("clay_ball_red");

    // === ГРАВИЙ (12) ===
    public static final DeferredItem<BlockItem> ANDESITE_GRAVEL = ITEMS.registerSimpleBlockItem("andesite_gravel", ModBlocks.ANDESITE_GRAVEL);
    public static final DeferredItem<BlockItem> BASALT_GRAVEL = ITEMS.registerSimpleBlockItem("basalt_gravel", ModBlocks.BASALT_GRAVEL);
    public static final DeferredItem<BlockItem> CHALK_GRAVEL = ITEMS.registerSimpleBlockItem("chalk_gravel", ModBlocks.CHALK_GRAVEL);
    public static final DeferredItem<BlockItem> DIORITE_GRAVEL = ITEMS.registerSimpleBlockItem("diorite_gravel", ModBlocks.DIORITE_GRAVEL);
    public static final DeferredItem<BlockItem> DOLOMITE_GRAVEL = ITEMS.registerSimpleBlockItem("dolomite_gravel", ModBlocks.DOLOMITE_GRAVEL);
    public static final DeferredItem<BlockItem> GABBRO_GRAVEL = ITEMS.registerSimpleBlockItem("gabbro_gravel", ModBlocks.GABBRO_GRAVEL);
    public static final DeferredItem<BlockItem> GRANITE_GRAVEL = ITEMS.registerSimpleBlockItem("granite_gravel", ModBlocks.GRANITE_GRAVEL);
    public static final DeferredItem<BlockItem> GRAY_GRANITE_GRAVEL = ITEMS.registerSimpleBlockItem("gray_granite_gravel", ModBlocks.GRAY_GRANITE_GRAVEL);
    public static final DeferredItem<BlockItem> LIMESTONE_GRAVEL = ITEMS.registerSimpleBlockItem("limestone_gravel", ModBlocks.LIMESTONE_GRAVEL);
    public static final DeferredItem<BlockItem> MARBLE_GRAVEL = ITEMS.registerSimpleBlockItem("marble_gravel", ModBlocks.MARBLE_GRAVEL);
    public static final DeferredItem<BlockItem> SANDSTONE_GRAVEL = ITEMS.registerSimpleBlockItem("sandstone_gravel", ModBlocks.SANDSTONE_GRAVEL);
    public static final DeferredItem<BlockItem> SHALE_GRAVEL = ITEMS.registerSimpleBlockItem("shale_gravel", ModBlocks.SHALE_GRAVEL);

    // Дёрнорез: добывает блоки вместе с дёрном
    public static final DeferredItem<TurfCutterItem> TURF_CUTTER = ITEMS.registerItem("turf_cutter",
            props -> new TurfCutterItem(Tiers.IRON, props.attributes(DiggerItem.createAttributes(Tiers.IRON, 1.0F, -2.8F))));

    // === ИНСТРУМЕНТЫ ===
    public static final DeferredItem<Item> FLINT_AXE = ITEMS.registerItem("flint_axe",
            props -> new AxeItem(Tiers.STONE, props.attributes(DiggerItem.createAttributes(Tiers.STONE, 7.0f, -3.2f))));
    public static final DeferredItem<Item> FLINT_SHOVEL = ITEMS.registerItem("flint_shovel",
            props -> new ShovelItem(Tiers.STONE, props.attributes(DiggerItem.createAttributes(Tiers.STONE, 1.5f, -3.0f))));
    public static final DeferredItem<Item> FLINT_PICKAXE = ITEMS.registerItem("flint_pickaxe",
            props -> new PickaxeItem(Tiers.STONE, props.attributes(DiggerItem.createAttributes(Tiers.STONE, 1.0f, -2.8f))));
    public static final DeferredItem<Item> COPPER_PICKAXE = ITEMS.registerItem("copper_pickaxe",
            props -> new PickaxeItem(ModTiers.COPPER, props.attributes(DiggerItem.createAttributes(ModTiers.COPPER, 1.0f, -2.8f))));
    public static final DeferredItem<Item> IRON_PICKAXE = ITEMS.registerItem("iron_pickaxe",
            props -> new PickaxeItem(Tiers.IRON, props.attributes(DiggerItem.createAttributes(Tiers.IRON, 1.0f, -2.8f))));
    /** Металлические топоры рубят любые брёвна (кремнёвый — только тонкие). */
    public static final DeferredItem<Item> COPPER_AXE = ITEMS.registerItem("copper_axe",
            props -> new AxeItem(ModTiers.COPPER, props.attributes(DiggerItem.createAttributes(ModTiers.COPPER, 6.5f, -3.15f))));
    public static final DeferredItem<Item> IRON_AXE = ITEMS.registerItem("iron_axe",
            props -> new AxeItem(Tiers.IRON, props.attributes(DiggerItem.createAttributes(Tiers.IRON, 6.0f, -3.1f))));

    // === КАМЕШКИ (12) ===
    public static final DeferredItem<BlockItem> ANDESITE_PEBBLE = ITEMS.registerSimpleBlockItem("andesite_pebble", ModBlocks.ANDESITE_PEBBLE);
    public static final DeferredItem<BlockItem> BASALT_PEBBLE = ITEMS.registerSimpleBlockItem("basalt_pebble", ModBlocks.BASALT_PEBBLE);
    public static final DeferredItem<BlockItem> CHALK_PEBBLE = ITEMS.registerSimpleBlockItem("chalk_pebble", ModBlocks.CHALK_PEBBLE);
    public static final DeferredItem<BlockItem> DIORITE_PEBBLE = ITEMS.registerSimpleBlockItem("diorite_pebble", ModBlocks.DIORITE_PEBBLE);
    public static final DeferredItem<BlockItem> DOLOMITE_PEBBLE = ITEMS.registerSimpleBlockItem("dolomite_pebble", ModBlocks.DOLOMITE_PEBBLE);
    public static final DeferredItem<BlockItem> GABBRO_PEBBLE = ITEMS.registerSimpleBlockItem("gabbro_pebble", ModBlocks.GABBRO_PEBBLE);
    public static final DeferredItem<BlockItem> GRANITE_PEBBLE = ITEMS.registerSimpleBlockItem("granite_pebble", ModBlocks.GRANITE_PEBBLE);
    public static final DeferredItem<BlockItem> GRAY_GRANITE_PEBBLE = ITEMS.registerSimpleBlockItem("gray_granite_pebble", ModBlocks.GRAY_GRANITE_PEBBLE);
    public static final DeferredItem<BlockItem> LIMESTONE_PEBBLE = ITEMS.registerSimpleBlockItem("limestone_pebble", ModBlocks.LIMESTONE_PEBBLE);
    public static final DeferredItem<BlockItem> MARBLE_PEBBLE = ITEMS.registerSimpleBlockItem("marble_pebble", ModBlocks.MARBLE_PEBBLE);
    public static final DeferredItem<BlockItem> SANDSTONE_PEBBLE = ITEMS.registerSimpleBlockItem("sandstone_pebble", ModBlocks.SANDSTONE_PEBBLE);
    public static final DeferredItem<BlockItem> SHALE_PEBBLE = ITEMS.registerSimpleBlockItem("shale_pebble", ModBlocks.SHALE_PEBBLE);

    // === ПАЛКИ ===
    /** Обычная ветка: 3 раза ПКМ по породе, и получается обработанная палка. */
    public static final DeferredItem<BranchStickItem> STICK_BRANCH_02 = ITEMS.registerItem("stick_branch_02", BranchStickItem::new);
    public static final DeferredItem<Item> STICK_PROCESSED_02 = ITEMS.registerSimpleItem("stick_processed_02");

    // === ПЕНЬ ДЛЯ РУБКИ ===
    public static final DeferredItem<BlockItem> CHOPPING_STUMP = ITEMS.registerSimpleBlockItem("chopping_stump", ModBlocks.CHOPPING_STUMP);

    // === КРЕМЕНЬ ===
    /** Новый кремень: ставится на землю как камушек (блок flint_pebble) и выпадает из него. */
    public static final DeferredItem<BlockItem> FLINT = ITEMS.registerItem("flint",
            props -> new BlockItem(ModBlocks.FLINT_PEBBLE.get(), props));

    // === МУСОР (пока без функционала) ===
    public static final DeferredItem<Item> METAL_SCRAP = ITEMS.registerSimpleItem("metal_scrap");
    public static final DeferredItem<Item> PAPER_SCRAPS = ITEMS.registerSimpleItem("paper_scraps");
    public static final DeferredItem<Item> PLASTIC_WASTE = ITEMS.registerSimpleItem("plastic_waste");
}

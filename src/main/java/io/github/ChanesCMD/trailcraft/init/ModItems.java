package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Tiers;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import io.github.ChanesCMD.trailcraft.item.PocketItem;
import io.github.ChanesCMD.trailcraft.item.CoinItem;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import io.github.ChanesCMD.trailcraft.item.WalletItem;
import io.github.ChanesCMD.trailcraft.item.BranchStickItem;
import io.github.ChanesCMD.trailcraft.item.ClothBlankItem;
import io.github.ChanesCMD.trailcraft.item.FirestarterItem;
import io.github.ChanesCMD.trailcraft.item.HammerItem;
import io.github.ChanesCMD.trailcraft.item.TurfCutterItem;
import io.github.ChanesCMD.trailcraft.farm.Farm;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.ArrayList;
import java.util.List;
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

    // === ГРЯДКИ (3): в выживании вспахиваются тяпкой из почвы или дёрна ===
    public static final DeferredItem<BlockItem> ZHELTOZEM_FARMLAND = ITEMS.registerSimpleBlockItem("zheltozem_farmland", ModBlocks.ZHELTOZEM_FARMLAND);
    public static final DeferredItem<BlockItem> BUROZEM_FARMLAND = ITEMS.registerSimpleBlockItem("burozem_farmland", ModBlocks.BUROZEM_FARMLAND);
    public static final DeferredItem<BlockItem> CHERNOZEM_FARMLAND = ITEMS.registerSimpleBlockItem("chernozem_farmland", ModBlocks.CHERNOZEM_FARMLAND);

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

    // === ДИКИЕ РАСТЕНИЯ (в выживании растут сами: с кустов падают ветки, с моркови и картофеля — морковь и картофель) ===
    public static final DeferredItem<BlockItem> CURRANT_BUSH = ITEMS.registerSimpleBlockItem("currant_bush", ModBlocks.CURRANT_BUSH);
    public static final DeferredItem<BlockItem> CLOUDBERRY_BUSH = ITEMS.registerSimpleBlockItem("cloudberry_bush", ModBlocks.CLOUDBERRY_BUSH);
    public static final DeferredItem<BlockItem> GOOSEBERRY_BUSH = ITEMS.registerSimpleBlockItem("gooseberry_bush", ModBlocks.GOOSEBERRY_BUSH);
    public static final DeferredItem<BlockItem> BLACKBERRY_BUSH = ITEMS.registerSimpleBlockItem("blackberry_bush", ModBlocks.BLACKBERRY_BUSH);
    public static final DeferredItem<BlockItem> WILD_CARROT = ITEMS.registerSimpleBlockItem("wild_carrot", ModBlocks.WILD_CARROT);
    public static final DeferredItem<BlockItem> WILD_POTATO = ITEMS.registerSimpleBlockItem("wild_potato", ModBlocks.WILD_POTATO);
    public static final DeferredItem<DoubleHighBlockItem> WILD_JUTE = ITEMS.registerItem("wild_jute",
            props -> new DoubleHighBlockItem(ModBlocks.WILD_JUTE.get(), props));

    // === ЯГОДЫ (еда, Farm.BERRY_FOOD) ===
    public static final DeferredItem<Item> CURRANT = ITEMS.registerItem("currant", props -> new Item(props.food(Farm.BERRY_FOOD)));
    public static final DeferredItem<Item> CLOUDBERRY = ITEMS.registerItem("cloudberry", props -> new Item(props.food(Farm.BERRY_FOOD)));
    public static final DeferredItem<Item> GOOSEBERRY = ITEMS.registerItem("gooseberry", props -> new Item(props.food(Farm.BERRY_FOOD)));
    public static final DeferredItem<Item> BLACKBERRY = ITEMS.registerItem("blackberry", props -> new Item(props.food(Farm.BERRY_FOOD)));

    // === ДЖУТ ===
    /** Волокно джута: из трёх — верёвка. */
    public static final DeferredItem<Item> JUTE = ITEMS.registerSimpleItem("jute");
    /** Семена джута: сажаются только на грядку (блок jute_crop). */
    public static final DeferredItem<ItemNameBlockItem> JUTE_SEEDS = ITEMS.registerItem("jute_seeds",
            props -> new ItemNameBlockItem(ModBlocks.JUTE_CROP.get(), props));
    public static final DeferredItem<Item> ROPE = ITEMS.registerSimpleItem("rope");
    /** Тканевая заготовка: 3 джута и обработанная палка; с палкой в другой руке ПКМ — тканевое полотно. */
    public static final DeferredItem<ClothBlankItem> CLOTH_BLANK = ITEMS.registerItem("cloth_blank", ClothBlankItem::new);
    /** Тканевое полотно: 3 полотна и верёвка — армейский вещмешок. */
    public static final DeferredItem<Item> CLOTH = ITEMS.registerSimpleItem("cloth");

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

    // Остальные инструменты (кирки, топоры, ножи, копья и т. д.) — в ModTools

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
    /** Обычная ветка: 3 раза ПКМ по породе, и получается обработанная палка; кладётся на землю (блок fallen_branch). */
    public static final DeferredItem<BranchStickItem> STICK_BRANCH = ITEMS.registerItem("stick_branch",
            props -> new BranchStickItem(ModBlocks.FALLEN_BRANCH.get(), props));
    public static final DeferredItem<Item> STICK_PROCESSED = ITEMS.registerSimpleItem("stick_processed");
    /** Длинная палка: две обработанные палки по диагонали; древко копий. */
    public static final DeferredItem<Item> LONG_STICK = ITEMS.registerSimpleItem("long_stick");
    /** Огниво: разжигает костёр из выброшенных тонкого бревна и 4 веток (FirestarterItem). */
    public static final DeferredItem<FirestarterItem> FIRESTARTER = ITEMS.registerItem("firestarter",
            props -> new FirestarterItem(props.durability(FirestarterItem.DURABILITY)));
    /** Каменный молоток: твёрдый камешек на обработанной палке, дробит куски руды в шихту бронз (HammerItem). */
    public static final DeferredItem<HammerItem> STONE_HAMMER = ITEMS.registerItem("stone_hammer",
            props -> new HammerItem(props.durability(HammerItem.DURABILITY)));

    // === ПЕНЬ ДЛЯ РУБКИ ===
    public static final DeferredItem<BlockItem> CHOPPING_STUMP = ITEMS.registerSimpleBlockItem("chopping_stump", ModBlocks.CHOPPING_STUMP);

    // === РЮКЗАКИ ===
    /** Тир 1 — армейский вещмешок, тир 2 — кожаный рюкзак, тир 3 — большой тактический. Отличаются числом карманов. */
    public static final DeferredItem<BackpackItem> DUFFEL_BACKPACK = ITEMS.registerItem("duffel_backpack",
            props -> new BackpackItem(ModBlocks.DUFFEL_BACKPACK.get(), props.stacksTo(1), BackpackItem.DUFFEL_POCKETS));
    public static final DeferredItem<BackpackItem> LEATHER_BACKPACK = ITEMS.registerItem("leather_backpack",
            props -> new BackpackItem(ModBlocks.LEATHER_BACKPACK.get(), props.stacksTo(1), BackpackItem.LEATHER_POCKETS));
    public static final DeferredItem<BackpackItem> TACTICAL_BACKPACK = ITEMS.registerItem("tactical_backpack",
            props -> new BackpackItem(ModBlocks.TACTICAL_BACKPACK.get(), props.stacksTo(1), BackpackItem.TACTICAL_POCKETS));

    // === СНАРЯЖЕНИЕ ИГРОКА ===
    /** Календарь: кладётся в слот календаря рюкзака (кнопка календаря в инвентаре, дата в HUD). */
    public static final DeferredItem<Item> CALENDAR = ITEMS.registerItem("calendar", props -> new Item(props.stacksTo(1)));
    /** Атлас: кладётся в слот атласа рюкзака (миникарта в HUD и полноэкранная карта). */
    public static final DeferredItem<Item> ATLAS = ITEMS.registerItem("atlas", props -> new Item(props.stacksTo(1)));
    /** Пояс: носится в слоте пояса, при смерти остаётся у игрока вместе с флягой (см. BeltHandler). */
    public static final DeferredItem<Item> BELT = ITEMS.registerItem("belt", props -> new Item(props.stacksTo(1)));
    /** Фляга для питьевой воды: кладётся в окно пояса. */
    public static final DeferredItem<FlaskItem> FLASK = ITEMS.registerItem("flask", props -> new FlaskItem(props.stacksTo(1)));
    /** Кошелёк на 8 видов монет: носится в слоте кошелька. */
    public static final DeferredItem<WalletItem> WALLET = ITEMS.registerItem("wallet", props -> new WalletItem(props.stacksTo(1)));
    /** Приборы на поясе (см. BeltHandler, client/Coords): высотомер показывает высоту полосой, секстант уточняет X */
    public static final DeferredItem<Item> BRASS_ALTIMETER = ITEMS.registerItem("brass_altimeter", props -> new Item(props.stacksTo(1)));
    public static final DeferredItem<Item> SEXTANT = ITEMS.registerItem("sextant", props -> new Item(props.stacksTo(1)));
    /** Хронометр уточняет Z до чанка. */
    public static final DeferredItem<Item> CHRONOMETER = ITEMS.registerItem("chronometer", props -> new Item(props.stacksTo(1)));
    /** GPS: в любом слоте приборов пояса даёт точные координаты, два других слота при нём закрыты. */
    public static final DeferredItem<Item> GPS = ITEMS.registerItem("gps", props -> new Item(props.stacksTo(1)));

    // === МОНЕТЫ (по возрастанию, каждая в 10 раз дороже предыдущей; в кошельке меняются сами) ===
    public static final List<DeferredItem<CoinItem>> COINS = registerCoins("copper_coin", "bronze_coin", "silver_coin", "gold_coin",
            "platinum_coin", "precious_platinum_coin", "iridium_coin", "precious_iridium_coin");

    // === КАРМАНЫ РЮКЗАКА ===
    public static final DeferredItem<PocketItem> SMALL_POCKET = ITEMS.registerItem("small_pocket",
            props -> new PocketItem(props.stacksTo(16), BackpackItem.SMALL_POCKET_SLOTS));
    public static final DeferredItem<PocketItem> MEDIUM_POCKET = ITEMS.registerItem("medium_pocket",
            props -> new PocketItem(props.stacksTo(16), BackpackItem.MEDIUM_POCKET_SLOTS));
    public static final DeferredItem<PocketItem> LARGE_POCKET = ITEMS.registerItem("large_pocket",
            props -> new PocketItem(props.stacksTo(16), BackpackItem.LARGE_POCKET_SLOTS));

    // === КРЕМЕНЬ ===
    /** Новый кремень: ставится на землю как камушек (блок flint_pebble) и выпадает из него. */
    public static final DeferredItem<BlockItem> FLINT = ITEMS.registerItem("flint",
            props -> new BlockItem(ModBlocks.FLINT_PEBBLE.get(), props));

    // === МУСОР (пока без функционала) ===
    public static final DeferredItem<Item> METAL_SCRAP = ITEMS.registerSimpleItem("metal_scrap");
    public static final DeferredItem<Item> PAPER_SCRAPS = ITEMS.registerSimpleItem("paper_scraps");
    public static final DeferredItem<Item> PLASTIC_WASTE = ITEMS.registerSimpleItem("plastic_waste");

    private static List<DeferredItem<CoinItem>> registerCoins(String... names) {
        List<DeferredItem<CoinItem>> coins = new ArrayList<>();
        for (int tier = 0; tier < names.length; tier++) {
            int t = tier;
            coins.add(ITEMS.registerItem(names[tier], props -> new CoinItem(props.stacksTo(99), t)));
        }
        return List.copyOf(coins);
    }
}

package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.smelting.BloomeryBlock;
import io.github.ChanesCMD.trailcraft.smelting.BloomeryBlockEntity;
import io.github.ChanesCMD.trailcraft.smelting.BloomeryMenu;
import io.github.ChanesCMD.trailcraft.smelting.ClayMoldItem;
import io.github.ChanesCMD.trailcraft.smelting.JugItem;
import io.github.ChanesCMD.trailcraft.smelting.JugMenu;
import io.github.ChanesCMD.trailcraft.smelting.Mold;
import io.github.ChanesCMD.trailcraft.smelting.MoldItem;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Глина, кирпичи и плавка металлов (пакет smelting): кирпичи четырёх глин, солома и саманный («укреплённый») кирпич,
 * малая сыродутная печь, слепок из глины, сырой и обожжённый кувшин, формы для литья и головки инструментов.
 */
public final class ModSmelting {
    /** Цвета глины (как у clay_ball_* и clay_*) и цвет обожжённого кирпича на карте. */
    public static final Map<String, MapColor> CLAYS = new LinkedHashMap<>();

    static {
        CLAYS.put("white", MapColor.TERRACOTTA_WHITE);
        CLAYS.put("blue", MapColor.TERRACOTTA_LIGHT_BLUE);
        CLAYS.put("gray", MapColor.TERRACOTTA_GRAY);
        CLAYS.put("red", MapColor.TERRACOTTA_RED);
    }

    /** Кирпич (предмет, обжиг комка глины на костре) и блок кирпичей (4 кирпича) для каждой глины. */
    public static final Map<String, DeferredItem<Item>> BRICKS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> BRICK_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> BRICK_BLOCK_ITEMS = new LinkedHashMap<>();

    static {
        CLAYS.forEach((clay, color) -> {
            BRICKS.put(clay, ModItems.ITEMS.registerSimpleItem("brick_" + clay));
            DeferredBlock<Block> block = ModBlocks.BLOCKS.registerSimpleBlock("bricks_" + clay,
                    BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(2.0f, 6.0f)
                            .sound(SoundType.STONE));
            BRICK_BLOCKS.put(clay, block);
            BRICK_BLOCK_ITEMS.put(clay, ModItems.ITEMS.registerSimpleBlockItem("bricks_" + clay, block));
        });
    }

    /** Солома: с травы, срезанной ножом, серпом или косой (StrawEvents). */
    public static final DeferredItem<Item> STRAW = ModItems.ITEMS.registerSimpleItem("straw");
    /** Необработанный укреплённый кирпич: любая глина и солома; на костре — укреплённый (саманный) кирпич. */
    public static final DeferredItem<Item> UNFIRED_REINFORCED_BRICK = ModItems.ITEMS.registerSimpleItem("unfired_reinforced_brick");
    public static final DeferredItem<Item> REINFORCED_BRICK = ModItems.ITEMS.registerSimpleItem("reinforced_brick");
    /** Слепок из глины: ПКМ — выбрать, что вылепить (ClayShape). */
    public static final DeferredItem<ClayMoldItem> CLAY_MOLD = ModItems.ITEMS.registerItem("clay_mold",
            p -> new ClayMoldItem(p.stacksTo(16)));
    /** Сырой кувшин: обжигается на костре в кувшин. */
    public static final DeferredItem<Item> UNFIRED_JUG = ModItems.ITEMS.registerItem("unfired_jug", p -> new Item(p.stacksTo(1)));
    /** Кувшин: в него кладут металлы и плавят в малой сыродутной печи. */
    public static final DeferredItem<JugItem> JUG = ModItems.ITEMS.registerItem("jug", p -> new JugItem(p.stacksTo(1)));

    /** Формы для литья (Mold): сырая (из слепка) обжигается на костре в форму. */
    public static final Map<Mold, DeferredItem<Item>> UNFIRED_MOLDS = new EnumMap<>(Mold.class);
    public static final Map<Mold, DeferredItem<MoldItem>> MOLDS = new EnumMap<>(Mold.class);
    /** Материалы инструментов, которые отливаются в формах (Metal.toolMaterial). */
    public static final List<String> CAST_MATERIALS = List.of("copper", "bronze");
    /** Головки инструментов из форм: ключ — «материал_инструмент» (copper_pickaxe), предмет — copper_pickaxe_head. */
    public static final Map<String, DeferredItem<Item>> HEADS = new LinkedHashMap<>();

    static {
        for (Mold mold : Mold.values()) {
            UNFIRED_MOLDS.put(mold, ModItems.ITEMS.registerItem("unfired_" + mold.id + "_mold", p -> new Item(p.stacksTo(1))));
            MOLDS.put(mold, ModItems.ITEMS.registerItem(mold.id + "_mold", p -> new MoldItem(mold, p.stacksTo(1))));
        }
        for (String material : CAST_MATERIALS) {
            for (Mold mold : Mold.values()) {
                if (mold != Mold.INGOT) {
                    String key = material + "_" + mold.id;
                    HEADS.put(key, ModItems.ITEMS.registerSimpleItem(key + "_head"));
                }
            }
        }
    }

    /** Малая сыродутная печь из укреплённых кирпичей. */
    public static final DeferredBlock<BloomeryBlock> SMALL_BLOOMERY = ModBlocks.BLOCKS.registerBlock("small_bloomery",
            BloomeryBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.5f, 6.0f)
                    .sound(SoundType.STONE).lightLevel(state -> state.getValue(BloomeryBlock.LIT) ? 13 : 0));
    public static final DeferredItem<BlockItem> SMALL_BLOOMERY_ITEM = ModItems.ITEMS.registerSimpleBlockItem("small_bloomery", SMALL_BLOOMERY);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloomeryBlockEntity>> BLOOMERY_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("small_bloomery",
                    () -> BlockEntityType.Builder.of(BloomeryBlockEntity::new, SMALL_BLOOMERY.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BloomeryMenu>> BLOOMERY_MENU =
            ModMenus.MENUS.register("small_bloomery", () -> new MenuType<>(BloomeryMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<JugMenu>> JUG_MENU =
            ModMenus.MENUS.register("jug", () -> new MenuType<>(JugMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Вызывается из конструктора мода, чтобы статические блоки выше выполнились до событий регистрации. */
    public static void init() {}

    private ModSmelting() {}
}

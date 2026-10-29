package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.bee.ApiaryBlock;
import io.github.ChanesCMD.trailcraft.bee.ApiaryBlockEntity;
import io.github.ChanesCMD.trailcraft.bee.ApiaryMenu;
import io.github.ChanesCMD.trailcraft.bee.BeeItem;
import io.github.ChanesCMD.trailcraft.bee.BeeSpecies;
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
import java.util.List;
import java.util.Map;

/**
 * Пчёлы (пакет bee, простой вариант Forestry): пчёлы по видам, дикие ульи (с них — пчёлы и соты), соты, воск, пасека.
 * JSON, текстуры и генерация ульев — scripts/gen_bees.py.
 */
public final class ModBees {
    public static final Map<BeeSpecies, DeferredItem<BeeItem>> BEES = new EnumMap<>(BeeSpecies.class);
    /** Дикие ульи — только у диких видов. */
    public static final Map<BeeSpecies, DeferredBlock<Block>> HIVES = new EnumMap<>(BeeSpecies.class);
    public static final Map<BeeSpecies, DeferredItem<BlockItem>> HIVE_ITEMS = new EnumMap<>(BeeSpecies.class);
    public static final List<String> COMBS = List.of("honey_comb", "wax_comb", "resin_comb", "sugar_comb");
    public static final List<DeferredItem<Item>> COMB_ITEMS;

    static {
        for (BeeSpecies species : BeeSpecies.values()) {
            BEES.put(species, ModItems.ITEMS.registerItem(species.id + "_bee", props -> new BeeItem(species, props)));
            if (species.wild()) {
                DeferredBlock<Block> hive = ModBlocks.BLOCKS.registerSimpleBlock(species.id + "_hive", BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_YELLOW).strength(0.3f).sound(SoundType.WOOD));
                HIVES.put(species, hive);
                HIVE_ITEMS.put(species, ModItems.ITEMS.registerSimpleBlockItem(species.id + "_hive", hive));
            }
        }
        COMB_ITEMS = COMBS.stream().map(ModItems.ITEMS::registerSimpleItem).toList();
    }

    /** Пчелиный воск: с сот на костре или в смесителе Create; с нитью — свеча. */
    public static final DeferredItem<Item> BEESWAX = ModItems.ITEMS.registerSimpleItem("beeswax");

    public static final DeferredBlock<ApiaryBlock> APIARY = ModBlocks.BLOCKS.registerBlock("apiary", ApiaryBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.6f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> APIARY_ITEM = ModItems.ITEMS.registerSimpleBlockItem("apiary", APIARY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiaryBlockEntity>> APIARY_ENTITY =
            ModBlockEntities.BLOCK_ENTITIES.register("apiary",
                    () -> BlockEntityType.Builder.of(ApiaryBlockEntity::new, APIARY.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ApiaryMenu>> APIARY_MENU =
            ModMenus.MENUS.register("apiary", () -> new MenuType<>(ApiaryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Вызывается из конструктора мода, чтобы статические блоки выше выполнились до событий регистрации. */
    public static void init() {}

    private ModBees() {}
}

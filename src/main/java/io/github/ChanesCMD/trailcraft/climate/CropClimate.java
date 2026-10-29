package io.github.ChanesCMD.trailcraft.climate;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Диапазон температуры культур (°C, средняя за сутки — Climate.mean): вне его культура не растёт (ClimateEvents,
 * FruitBlock, FruitLeavesBlock). Диапазон виден в подсказке семян, саженцев и плодов.
 */
public final class CropClimate {
    public record Range(int min, int max) {
        public boolean contains(float t) {
            return t >= min && t <= max;
        }
    }

    private static Map<Block, Range> byBlock;
    private static Map<Item, Range> byItem;

    private CropClimate() {}

    private static void init() {
        if (byBlock != null) {
            return;
        }
        Map<Block, Range> blocks = new HashMap<>();
        Map<Item, Range> items = new HashMap<>();
        // культура: блок, предметы, которыми её сажают (и плод), диапазон
        add(blocks, items, ModBlocks.WHEAT, 4, 32, ModItems.WHEAT_SEEDS);
        add(blocks, items, ModBlocks.SUGAR_BEET, 5, 30, ModItems.SUGAR_BEET_SEEDS);
        add(blocks, items, () -> Blocks.CARROTS, 5, 30, () -> Items.CARROT);
        add(blocks, items, () -> Blocks.POTATOES, 7, 28, () -> Items.POTATO);
        add(blocks, items, ModBlocks.JUTE_CROP, 15, 38, ModItems.JUTE_SEEDS);
        add(blocks, items, ModBlocks.SUGAR_CANE, 18, 40, ModItems.SUGAR_CANE);
        add(blocks, items, ModBlocks.CACTUS, 12, 48, ModItems.CACTUS);
        add(blocks, items, ModBlocks.COCOA, 20, 40, ModItems.COCOA_BEANS);
        add(blocks, items, ModBlocks.CURRANT_BUSH, 2, 28, ModItems.CURRANT_BUSH);
        add(blocks, items, ModBlocks.CLOUDBERRY_BUSH, 0, 22, ModItems.CLOUDBERRY_BUSH);
        add(blocks, items, ModBlocks.GOOSEBERRY_BUSH, 3, 28, ModItems.GOOSEBERRY_BUSH);
        add(blocks, items, ModBlocks.BLACKBERRY_BUSH, 8, 32, ModItems.BLACKBERRY_BUSH);
        // фруктовые деревья: плод под листвой (FruitBlock) и листва, которая его завязывает
        Map<String, Range> fruits = Map.of("apple", new Range(5, 30), "pear", new Range(7, 30), "plum", new Range(6, 30),
                "cherry", new Range(6, 28), "apricot", new Range(10, 34));
        for (ModWood.WoodSet set : ModWood.SETS) {
            Range range = fruits.get(set.id);
            if (range != null && set.fruit != null) {
                blocks.put(set.fruit.get(), range);
                blocks.put(set.leaves.get(), range);
                items.put(set.sapling.get().asItem(), range);
                items.put(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, set.id)), range);
            }
        }
        items.remove(Items.AIR);
        byItem = items;
        byBlock = blocks;
    }

    private static void add(Map<Block, Range> blocks, Map<Item, Range> items, Supplier<? extends Block> block, int min,
                            int max, Supplier<? extends Item> item) {
        Range range = new Range(min, max);
        blocks.put(block.get(), range);
        items.put(item.get(), range);
    }

    public static @Nullable Range of(Block block) {
        init();
        return byBlock.get(block);
    }

    public static @Nullable Range of(Item item) {
        init();
        return byItem.get(item);
    }

    /** Может ли культура расти здесь сейчас; блок без диапазона — может всегда. */
    public static boolean canGrow(Level level, BlockPos pos, Block block) {
        Range range = of(block);
        return range == null || range.contains(Climate.mean(level, pos));
    }
}

package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TrailCraft.MODID);

    // === КАМНИ (12 блоков) ===
    public static final DeferredBlock<Block> ANDESITE = BLOCKS.registerSimpleBlock("andesite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> BASALT = BLOCKS.registerSimpleBlock("basalt",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> CHALK = BLOCKS.registerSimpleBlock("chalk",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f, 0.8f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> DIORITE = BLOCKS.registerSimpleBlock("diorite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> DOLOMITE = BLOCKS.registerSimpleBlock("dolomite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> GABBRO = BLOCKS.registerSimpleBlock("gabbro",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> GRANITE = BLOCKS.registerSimpleBlock("granite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> GRAY_GRANITE = BLOCKS.registerSimpleBlock("gray_granite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> LIMESTONE = BLOCKS.registerSimpleBlock("limestone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> MARBLE = BLOCKS.registerSimpleBlock("marble",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 6.0f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> SANDSTONE = BLOCKS.registerSimpleBlock("sandstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.8f, 0.8f).sound(SoundType.STONE));

    public static final DeferredBlock<Block> SHALE = BLOCKS.registerSimpleBlock("shale",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).sound(SoundType.STONE));

    // === БАЗОВЫЕ БЛОКИ (8 блоков) ===
    public static final DeferredBlock<Block> ZHELTOZEM = BLOCKS.registerSimpleBlock("zheltozem",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> BUROZEM = BLOCKS.registerSimpleBlock("burozem",
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CHERNOZEM = BLOCKS.registerSimpleBlock("chernozem",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> LOAM = BLOCKS.registerSimpleBlock("loam",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CLAY_WHITE = BLOCKS.registerSimpleBlock("clay_white",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CLAY_BLUE = BLOCKS.registerSimpleBlock("clay_blue",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CLAY_GRAY = BLOCKS.registerSimpleBlock("clay_gray",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> PEAT = BLOCKS.registerSimpleBlock("peat",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.GRAVEL));

    // === ТРАВЯНЫЕ ПАРЫ (8 блоков) ===
    public static final DeferredBlock<Block> ZHELTOZEM_GRASS = BLOCKS.registerSimpleBlock("zheltozem_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> BUROZEM_GRASS = BLOCKS.registerSimpleBlock("burozem_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> CHERNOZEM_GRASS = BLOCKS.registerSimpleBlock("chernozem_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> LOAM_GRASS = BLOCKS.registerSimpleBlock("loam_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> CLAY_WHITE_GRASS = BLOCKS.registerSimpleBlock("clay_white_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> CLAY_BLUE_GRASS = BLOCKS.registerSimpleBlock("clay_blue_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> CLAY_GRAY_GRASS = BLOCKS.registerSimpleBlock("clay_gray_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> PEAT_GRASS = BLOCKS.registerSimpleBlock("peat_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.GRASS));

    // === ПЕСОК (3 блока) ===
    public static final DeferredBlock<Block> SAND_WHITE = BLOCKS.registerSimpleBlock("sand_white",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredBlock<Block> SAND_YELLOW = BLOCKS.registerSimpleBlock("sand_yellow",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredBlock<Block> SAND_DARK = BLOCKS.registerSimpleBlock("sand_dark",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.SAND));

    // === ИЛ (1 блок) ===
    public static final DeferredBlock<Block> SILT = BLOCKS.registerSimpleBlock("silt",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.GRAVEL));

    // === ТРАВА (3 блока) ===
    public static final DeferredBlock<Block> GRASS_MEDIUM = BLOCKS.registerSimpleBlock("grass_medium",
            BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> GRASS_SMALL = BLOCKS.registerSimpleBlock("grass_small",
            BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> GRASS_TALL = BLOCKS.registerSimpleBlock("grass_tall",
            BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6f).sound(SoundType.GRASS));
}
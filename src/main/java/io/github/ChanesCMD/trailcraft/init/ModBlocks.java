package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import io.github.ChanesCMD.trailcraft.block.PebbleBlock;
import io.github.ChanesCMD.trailcraft.block.BackpackBlock;
import io.github.ChanesCMD.trailcraft.block.GraveBlock;
import io.github.ChanesCMD.trailcraft.block.ChoppingStumpBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import io.github.ChanesCMD.trailcraft.block.GrassPlantBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TrailCraft.MODID);

    // === КАМНИ (12 блоков) ===
    public static final DeferredBlock<Block> ANDESITE = BLOCKS.registerSimpleBlock("andesite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 12.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> BASALT = BLOCKS.registerBlock("basalt", RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0f, 16.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> CHALK = BLOCKS.registerSimpleBlock("chalk",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f, 3.2f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> DIORITE = BLOCKS.registerSimpleBlock("diorite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 12.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> DOLOMITE = BLOCKS.registerSimpleBlock("dolomite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 10.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> GABBRO = BLOCKS.registerSimpleBlock("gabbro",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(4.0f, 16.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> GRANITE = BLOCKS.registerSimpleBlock("granite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f, 14.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> GRAY_GRANITE = BLOCKS.registerSimpleBlock("gray_granite",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f, 14.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> LIMESTONE = BLOCKS.registerSimpleBlock("limestone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> MARBLE = BLOCKS.registerSimpleBlock("marble",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.5f, 10.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> SANDSTONE = BLOCKS.registerSimpleBlock("sandstone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0f, 4.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final DeferredBlock<Block> SHALE = BLOCKS.registerSimpleBlock("shale",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));

    // === БАЗОВЫЕ БЛОКИ (9 блоков) ===
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

    public static final DeferredBlock<Block> CLAY_RED = BLOCKS.registerSimpleBlock("clay_red",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CLAY_GRAY = BLOCKS.registerSimpleBlock("clay_gray",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> PEAT = BLOCKS.registerSimpleBlock("peat",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.GRAVEL));

    // === ТРАВЯНЫЕ ПАРЫ (9 блоков) ===
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

    public static final DeferredBlock<Block> CLAY_RED_GRASS = BLOCKS.registerSimpleBlock("clay_red_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> CLAY_GRAY_GRASS = BLOCKS.registerSimpleBlock("clay_gray_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.GRASS));

    public static final DeferredBlock<Block> PEAT_GRASS = BLOCKS.registerSimpleBlock("peat_grass",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.GRASS));

    // === ПЕСОК (4 блока) ===
    public static final DeferredBlock<Block> SAND_WHITE = BLOCKS.registerBlock("sand_white",
            props -> new ColoredFallingBlock(new ColorRGBA(0xE6E4DC), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredBlock<Block> SAND_YELLOW = BLOCKS.registerBlock("sand_yellow",
            props -> new ColoredFallingBlock(new ColorRGBA(0xDAD0A2), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredBlock<Block> SAND_GRAY = BLOCKS.registerBlock("sand_gray",
            props -> new ColoredFallingBlock(new ColorRGBA(0x4F4F52), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.SAND));

    public static final DeferredBlock<Block> SAND_RED = BLOCKS.registerBlock("sand_red",
            props -> new ColoredFallingBlock(new ColorRGBA(0xCF987B), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f).sound(SoundType.SAND));

    // === ИЛ (1 блок) ===
    public static final DeferredBlock<Block> SILT = BLOCKS.registerSimpleBlock("silt",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.GRAVEL));

    // === ГРАВИЙ ИЗ ПОРОД (12 блоков, падает как обычный гравий) ===
    public static final DeferredBlock<Block> ANDESITE_GRAVEL = BLOCKS.registerBlock("andesite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x838382), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> BASALT_GRAVEL = BLOCKS.registerBlock("basalt_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x46464B), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> CHALK_GRAVEL = BLOCKS.registerBlock("chalk_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xE2E1DB), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> DIORITE_GRAVEL = BLOCKS.registerBlock("diorite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xB4B4B4), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> DOLOMITE_GRAVEL = BLOCKS.registerBlock("dolomite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xCBC5BA), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> GABBRO_GRAVEL = BLOCKS.registerBlock("gabbro_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x414644), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> GRANITE_GRAVEL = BLOCKS.registerBlock("granite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x966958), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> GRAY_GRANITE_GRAVEL = BLOCKS.registerBlock("gray_granite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x5B5553), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> LIMESTONE_GRAVEL = BLOCKS.registerBlock("limestone_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xCFC39A), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> MARBLE_GRAVEL = BLOCKS.registerBlock("marble_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xC1C4C9), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> SANDSTONE_GRAVEL = BLOCKS.registerBlock("sandstone_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xB9996F), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6f).sound(SoundType.GRAVEL));

    public static final DeferredBlock<Block> SHALE_GRAVEL = BLOCKS.registerBlock("shale_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x575B62), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL));

    // === КАМЕШКИ (12 блоков, 3x5x2 пикселя) ===
    public static final DeferredBlock<Block> ANDESITE_PEBBLE = BLOCKS.registerBlock("andesite_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> BASALT_PEBBLE = BLOCKS.registerBlock("basalt_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> CHALK_PEBBLE = BLOCKS.registerBlock("chalk_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> DIORITE_PEBBLE = BLOCKS.registerBlock("diorite_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> DOLOMITE_PEBBLE = BLOCKS.registerBlock("dolomite_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> GABBRO_PEBBLE = BLOCKS.registerBlock("gabbro_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> GRANITE_PEBBLE = BLOCKS.registerBlock("granite_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> GRAY_GRANITE_PEBBLE = BLOCKS.registerBlock("gray_granite_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> LIMESTONE_PEBBLE = BLOCKS.registerBlock("limestone_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> MARBLE_PEBBLE = BLOCKS.registerBlock("marble_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> SANDSTONE_PEBBLE = BLOCKS.registerBlock("sandstone_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> SHALE_PEBBLE = BLOCKS.registerBlock("shale_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    /** Упавшая ветка на земле. Своего предмета нет: при разрушении выпадает ветка (stick_branch). */
    public static final DeferredBlock<Block> FALLEN_BRANCH = BLOCKS.registerBlock("fallen_branch", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instabreak().sound(SoundType.WOOD).noCollission().noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));

    /** Пень для рубки: бревно на нём раскалывается топором на 2 тонких. */
    public static final DeferredBlock<Block> CHOPPING_STUMP = BLOCKS.registerBlock("chopping_stump", ChoppingStumpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).ignitedByLava());

    /** Кремень на земле. Своего предмета у блока нет: ставится и выпадает как предмет trailcraft:flint. */
    public static final DeferredBlock<Block> FLINT_PEBBLE = BLOCKS.registerBlock("flint_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    // === РЮКЗАК И МОГИЛА ===
    /** Тестовый рюкзак, стоящий на земле (выброшенный). Предмет — ModItems.TEST_BACKPACK. */
    public static final DeferredBlock<Block> TEST_BACKPACK = BLOCKS.registerBlock("test_backpack", BackpackBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** Могила с вещами умершего игрока. Не взрывается и не сдвигается поршнем. */
    public static final DeferredBlock<Block> GRAVE = BLOCKS.registerBlock("grave", GraveBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 3600000.0f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));

    // === ТРАВА (3 блока, настоящие растения с крестообразной моделью) ===
    private static BlockBehaviour.Properties plantProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .replaceable()
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<Block> GRASS_SMALL = BLOCKS.registerBlock("grass_small",
            props -> new GrassPlantBlock(props, 6), plantProps());

    public static final DeferredBlock<Block> GRASS_MEDIUM = BLOCKS.registerBlock("grass_medium",
            props -> new GrassPlantBlock(props, 10), plantProps());

    public static final DeferredBlock<Block> GRASS_TALL = BLOCKS.registerBlock("grass_tall",
            props -> new GrassPlantBlock(props, 14), plantProps());
}

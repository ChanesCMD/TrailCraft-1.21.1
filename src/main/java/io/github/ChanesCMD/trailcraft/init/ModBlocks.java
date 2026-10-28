package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.util.ColorRGBA;
import net.minecraft.resources.ResourceLocation;
import io.github.ChanesCMD.trailcraft.ore.Rock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import io.github.ChanesCMD.trailcraft.block.PebbleBlock;
import io.github.ChanesCMD.trailcraft.block.SupportBlock;
import io.github.ChanesCMD.trailcraft.block.BackpackBlock;
import io.github.ChanesCMD.trailcraft.block.TurfBlock;
import io.github.ChanesCMD.trailcraft.block.GraveBlock;
import io.github.ChanesCMD.trailcraft.block.ChoppingStumpBlock;
import io.github.ChanesCMD.trailcraft.block.TrailCampfireBlock;
import io.github.ChanesCMD.trailcraft.block.BerryBushBlock;
import io.github.ChanesCMD.trailcraft.block.GardenBedBlock;
import io.github.ChanesCMD.trailcraft.block.JuteCropBlock;
import io.github.ChanesCMD.trailcraft.farm.Farm;
import net.minecraft.world.level.block.DoublePlantBlock;
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

    // === ТРАВЯНЫЕ ПАРЫ (9 блоков): дёрн на почве, зарастает соседнюю почву и отмирает под блоком (TurfBlock) ===
    public static final DeferredBlock<Block> ZHELTOZEM_GRASS = BLOCKS.registerBlock("zheltozem_grass", props -> new TurfBlock(props, ZHELTOZEM),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> BUROZEM_GRASS = BLOCKS.registerBlock("burozem_grass", props -> new TurfBlock(props, BUROZEM),
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> CHERNOZEM_GRASS = BLOCKS.registerBlock("chernozem_grass", props -> new TurfBlock(props, CHERNOZEM),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> LOAM_GRASS = BLOCKS.registerBlock("loam_grass", props -> new TurfBlock(props, LOAM),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> CLAY_WHITE_GRASS = BLOCKS.registerBlock("clay_white_grass", props -> new TurfBlock(props, CLAY_WHITE),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> CLAY_BLUE_GRASS = BLOCKS.registerBlock("clay_blue_grass", props -> new TurfBlock(props, CLAY_BLUE),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> CLAY_RED_GRASS = BLOCKS.registerBlock("clay_red_grass", props -> new TurfBlock(props, CLAY_RED),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> CLAY_GRAY_GRASS = BLOCKS.registerBlock("clay_gray_grass", props -> new TurfBlock(props, CLAY_GRAY),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6f).sound(SoundType.GRASS).randomTicks());

    public static final DeferredBlock<Block> PEAT_GRASS = BLOCKS.registerBlock("peat_grass", props -> new TurfBlock(props, PEAT),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.GRASS).randomTicks());

    // === ГРЯДКИ (3 блока): вспахиваются тяпкой из почвы или дёрна, без воды пересыхают в свою почву (GardenBedBlock) ===
    public static final DeferredBlock<Block> ZHELTOZEM_FARMLAND = registerFarmland("zheltozem_farmland", ZHELTOZEM,
            Farm.ZHELTOZEM_GROWTH, MapColor.COLOR_YELLOW);
    public static final DeferredBlock<Block> BUROZEM_FARMLAND = registerFarmland("burozem_farmland", BUROZEM,
            Farm.BUROZEM_GROWTH, MapColor.DIRT);
    public static final DeferredBlock<Block> CHERNOZEM_FARMLAND = registerFarmland("chernozem_farmland", CHERNOZEM,
            Farm.CHERNOZEM_GROWTH, MapColor.COLOR_BLACK);

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

    // === ГРАВИЙ ИЗ ПОРОД (12 блоков, падает как обычный гравий; добывается только киркой) ===
    public static final DeferredBlock<Block> ANDESITE_GRAVEL = BLOCKS.registerBlock("andesite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x838382), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> BASALT_GRAVEL = BLOCKS.registerBlock("basalt_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x46464B), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> CHALK_GRAVEL = BLOCKS.registerBlock("chalk_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xE2E1DB), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DIORITE_GRAVEL = BLOCKS.registerBlock("diorite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xB4B4B4), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> DOLOMITE_GRAVEL = BLOCKS.registerBlock("dolomite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xCBC5BA), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> GABBRO_GRAVEL = BLOCKS.registerBlock("gabbro_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x414644), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> GRANITE_GRAVEL = BLOCKS.registerBlock("granite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x966958), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> GRAY_GRANITE_GRAVEL = BLOCKS.registerBlock("gray_granite_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x5B5553), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> LIMESTONE_GRAVEL = BLOCKS.registerBlock("limestone_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xCFC39A), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> MARBLE_GRAVEL = BLOCKS.registerBlock("marble_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xC1C4C9), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> SANDSTONE_GRAVEL = BLOCKS.registerBlock("sandstone_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0xB9996F), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    public static final DeferredBlock<Block> SHALE_GRAVEL = BLOCKS.registerBlock("shale_gravel",
            props -> new ColoredFallingBlock(new ColorRGBA(0x575B62), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    // === БУЛЫЖНИК ИЗ ПОРОД (12 блоков, свойства — как у своей породы): нижние две трети обвала (collapse/Collapse) ===
    public static final Map<Rock, DeferredBlock<Block>> COBBLESTONES = new EnumMap<>(Rock.class);
    static {
        for (Rock rock : Rock.values()) {
            DeferredBlock<Block> stone = DeferredBlock.createBlock(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, rock.getSerializedName()));
            COBBLESTONES.put(rock, BLOCKS.register(rock.getSerializedName() + "_cobblestone",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(stone.get()))));
        }
    }

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

    /** Упавшая ветка на земле. Своего предмета нет: ставится и выпадает как ветка (stick_branch). */
    public static final DeferredBlock<Block> FALLEN_BRANCH = BLOCKS.registerBlock("fallen_branch", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instabreak().sound(SoundType.WOOD).noCollission().noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));

    /** Пень для рубки: бревно на нём раскалывается топором на 4 тонких ({@link ChoppingStumpBlock#THIN_LOGS}). */
    public static final DeferredBlock<Block> CHOPPING_STUMP = BLOCKS.registerBlock("chopping_stump", ChoppingStumpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).ignitedByLava());

    /** Костёр: зола и 0–4 тонких бревна; светит тем ярче, чем больше брёвен горит. Лут — в коде (TrailCampfireBlock). */
    public static final DeferredBlock<TrailCampfireBlock> CAMPFIRE = BLOCKS.registerBlock("campfire", TrailCampfireBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.5f).sound(SoundType.WOOD).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(TrailCampfireBlock.LIT) ? 11 + state.getValue(TrailCampfireBlock.LOGS) : 0));

    // === ОПОРЫ (collapse/Collapse): стойка и перекладина, ставятся одним предметом (SupportItem) ===
    /** Деревянная опора держит породу и почву 9×5×9 вокруг себя в раме, 3×5×3 — одна. */
    public static final DeferredBlock<SupportBlock> WOODEN_SUPPORT = registerSupport("wooden_support", false, 4, 1, 2, woodSupportProps());
    public static final DeferredBlock<SupportBlock> WOODEN_SUPPORT_BEAM = registerSupport("wooden_support_beam", true, 4, 1, 2, woodSupportProps());
    /** Железная опора держит 13×7×13 в раме, 5×7×5 — одна. */
    public static final DeferredBlock<SupportBlock> IRON_SUPPORT = registerSupport("iron_support", false, 6, 2, 3, ironSupportProps());
    public static final DeferredBlock<SupportBlock> IRON_SUPPORT_BEAM = registerSupport("iron_support_beam", true, 6, 2, 3, ironSupportProps());

    /** Кремень на земле. Своего предмета у блока нет: ставится и выпадает как предмет trailcraft:flint. */
    public static final DeferredBlock<Block> FLINT_PEBBLE = BLOCKS.registerBlock("flint_pebble", PebbleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2f).sound(SoundType.STONE).noCollission().noOcclusion().pushReaction(PushReaction.DESTROY));

    // === РЮКЗАК И МОГИЛА ===
    /** Рюкзаки, стоящие на земле (выброшенные). Предметы — одноимённые в ModItems. */
    public static final DeferredBlock<Block> DUFFEL_BACKPACK = registerBackpack("duffel_backpack", MapColor.COLOR_GREEN);
    public static final DeferredBlock<Block> LEATHER_BACKPACK = registerBackpack("leather_backpack", MapColor.COLOR_BROWN);
    public static final DeferredBlock<Block> TACTICAL_BACKPACK = registerBackpack("tactical_backpack", MapColor.COLOR_GRAY);

    /** Могила с вещами умершего игрока. Не взрывается и не сдвигается поршнем. */
    public static final DeferredBlock<Block> GRAVE = BLOCKS.registerBlock("grave", GraveBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 3600000.0f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .forceSolidOn());

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

    // === ДИКИЕ РАСТЕНИЯ ===
    /** Ягодные кусты: ПКМ по кусту с ягодами — ягоды, сломанный куст даёт ветки. Морошка низкая. */
    public static final DeferredBlock<Block> CURRANT_BUSH = BLOCKS.registerBlock("currant_bush",
            props -> new BerryBushBlock(props, ModItems.CURRANT, 16), bushProps());
    public static final DeferredBlock<Block> CLOUDBERRY_BUSH = BLOCKS.registerBlock("cloudberry_bush",
            props -> new BerryBushBlock(props, ModItems.CLOUDBERRY, 9), bushProps());
    public static final DeferredBlock<Block> GOOSEBERRY_BUSH = BLOCKS.registerBlock("gooseberry_bush",
            props -> new BerryBushBlock(props, ModItems.GOOSEBERRY, 16), bushProps());
    public static final DeferredBlock<Block> BLACKBERRY_BUSH = BLOCKS.registerBlock("blackberry_bush",
            props -> new BerryBushBlock(props, ModItems.BLACKBERRY, 16), bushProps());

    /** Дикие морковь и картофель: с них падают ванильные морковь и картофель (сажаются только на грядку). */
    public static final DeferredBlock<Block> WILD_CARROT = BLOCKS.registerBlock("wild_carrot",
            props -> new GrassPlantBlock(props, 14), wildPlantProps());
    public static final DeferredBlock<Block> WILD_POTATO = BLOCKS.registerBlock("wild_potato",
            props -> new GrassPlantBlock(props, 12), wildPlantProps());

    /** Дикий джут: в два блока высотой, с нижней половины падают джут и семена джута. */
    public static final DeferredBlock<Block> WILD_JUTE = BLOCKS.registerBlock("wild_jute", DoublePlantBlock::new, wildPlantProps());
    /** Посаженный джут (из семян, на грядке). Своего предмета нет: ставится и выпадает как семена (jute_seeds). */
    public static final DeferredBlock<Block> JUTE_CROP = BLOCKS.registerBlock("jute_crop", JuteCropBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY));

    private static BlockBehaviour.Properties bushProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().noCollission()
                .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties wildPlantProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<Block> registerFarmland(String name, DeferredBlock<Block> soil, float growthSpeed, MapColor color) {
        return BLOCKS.registerBlock(name, props -> new GardenBedBlock(props, soil, growthSpeed),
                BlockBehaviour.Properties.of().mapColor(color).randomTicks().strength(0.6f).sound(SoundType.GRAVEL)
                        .isViewBlocking((state, level, pos) -> true).isSuffocating((state, level, pos) -> true)
                        .pushReaction(PushReaction.DESTROY));
    }

    private static DeferredBlock<SupportBlock> registerSupport(String name, boolean beam, int horizontal, int loneHorizontal,
                                                               int vertical, BlockBehaviour.Properties properties) {
        return BLOCKS.registerBlock(name, props -> new SupportBlock(props, beam, horizontal, loneHorizontal, vertical), properties);
    }

    private static BlockBehaviour.Properties woodSupportProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion()
                .ignitedByLava().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties ironSupportProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<Block> registerBackpack(String name, MapColor color) {
        return BLOCKS.registerBlock(name, BackpackBlock::new,
                BlockBehaviour.Properties.of().mapColor(color).strength(0.5f).sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.DESTROY));
    }
}

package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.StrippableLogBlock;
import io.github.ChanesCMD.trailcraft.block.ThinLogBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 23 вида деревьев: по 16 блоков на вид (бревно, тонкое бревно, древесина, обтёсанные, листва, саженец, доски, ступени, плита, забор, калитка, дверь, люк, кнопка, нажимная плита).
 * Плюс красный клён: только своя листва и саженец, древесина — клёна (addLeafVariant).
 */
public final class ModWood {
    public static final class WoodSet {
        public final String id;
        public DeferredBlock<Block> log, thinLog, strippedLog, wood, strippedWood, leaves, sapling, planks, stairs, slab, fence, gate, door, trapdoor, button, plate;
        public final List<DeferredItem<? extends Item>> items = new ArrayList<>();
        WoodSet(String id) { this.id = id; }
    }

    public static final List<WoodSet> SETS = new ArrayList<>();

    /**
     * Прочность обычных брёвен и древесины: в 2 раза больше прежней (2.0), добываются вдвое дольше.
     * Все брёвна требуют топор (рукой ничего не выпадает); полные брёвна — топор лучше каменного
     * (см. теги incorrect_for_wooden/stone/gold_tool), тонкие можно рубить любым топором.
     */
    private static final float LOG_HARDNESS = 4.0f;

    static {
        add("oak", MapColor.WOOD, MapColor.PODZOL);
        add("birch", MapColor.SAND, MapColor.TERRACOTTA_WHITE);
        add("acacia", MapColor.COLOR_ORANGE, MapColor.STONE);
        add("sakura", MapColor.COLOR_PINK, MapColor.TERRACOTTA_BROWN);
        add("eucalyptus", MapColor.TERRACOTTA_YELLOW, MapColor.STONE);
        add("chestnut", MapColor.COLOR_BROWN, MapColor.PODZOL);
        add("thuja", MapColor.TERRACOTTA_ORANGE, MapColor.TERRACOTTA_BROWN);
        add("pine", MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_BROWN);
        add("fir", MapColor.SAND, MapColor.PODZOL);
        add("spruce", MapColor.PODZOL, MapColor.COLOR_BROWN);
        add("white_poplar", MapColor.SAND, MapColor.TERRACOTTA_WHITE);
        add("ebony", MapColor.COLOR_BLACK, MapColor.COLOR_BLACK);
        add("maple", MapColor.COLOR_ORANGE, MapColor.COLOR_BROWN);
        addLeafVariant("red_maple", "maple");
        add("cedar", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_ORANGE);
        add("willow", MapColor.COLOR_YELLOW, MapColor.COLOR_GRAY);
        add("lilac", MapColor.COLOR_PURPLE, MapColor.COLOR_BROWN);
        add("palm", MapColor.SAND, MapColor.COLOR_BROWN);
        add("ironwood", MapColor.STONE, MapColor.COLOR_GRAY);
        add("aspen", MapColor.SAND, MapColor.TERRACOTTA_WHITE);
        add("linden", MapColor.SAND, MapColor.COLOR_BROWN);
        add("pussy_willow", MapColor.COLOR_BROWN, MapColor.COLOR_BROWN);
        add("walnut", MapColor.COLOR_BROWN, MapColor.PODZOL);
        add("alder", MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_BROWN);
    }

    /** Вызывается из конструктора мода, чтобы статический блок выше выполнился до событий регистрации. */
    public static void init() {}

    private static DeferredBlock<Block> reg(String name, Function<BlockBehaviour.Properties, ? extends Block> factory, BlockBehaviour.Properties props) {
        return ModBlocks.BLOCKS.<Block>registerBlock(name, factory, props);
    }

    private static BlockBehaviour.Properties wood(MapColor color, float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance).sound(SoundType.WOOD).ignitedByLava();
    }

    private static void add(String id, MapColor planksColor, MapColor barkColor) {
        WoodSet s = new WoodSet(id);

        s.strippedLog = reg("stripped_" + id + "_log", RotatedPillarBlock::new, wood(planksColor, LOG_HARDNESS, 2.0f).requiresCorrectToolForDrops());
        s.strippedWood = reg("stripped_" + id + "_wood", RotatedPillarBlock::new, wood(planksColor, LOG_HARDNESS, 2.0f).requiresCorrectToolForDrops());
        s.log = reg(id + "_log", p -> new StrippableLogBlock(() -> s.strippedLog.get(), p), wood(barkColor, LOG_HARDNESS, 2.0f).requiresCorrectToolForDrops());
        // Тонкое бревно: характеристики как у прежних брёвен (прочность 2.0)
        s.thinLog = reg("thin_" + id + "_log", ThinLogBlock::new, wood(barkColor, 2.0f, 2.0f).noOcclusion().requiresCorrectToolForDrops());
        s.wood = reg(id + "_wood", p -> new StrippableLogBlock(() -> s.strippedWood.get(), p), wood(barkColor, LOG_HARDNESS, 2.0f).requiresCorrectToolForDrops());

        s.leaves = leaves(id);
        s.sapling = sapling(id);

        s.planks = ModBlocks.BLOCKS.registerSimpleBlock(id + "_planks", wood(planksColor, 2.0f, 3.0f));
        s.stairs = reg(id + "_stairs", p -> new StairBlock(s.planks.get().defaultBlockState(), p), wood(planksColor, 2.0f, 3.0f));
        s.slab = reg(id + "_slab", SlabBlock::new, wood(planksColor, 2.0f, 3.0f));
        s.fence = reg(id + "_fence", FenceBlock::new, wood(planksColor, 2.0f, 3.0f).forceSolidOn());
        s.gate = reg(id + "_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), wood(planksColor, 2.0f, 3.0f).forceSolidOn());
        s.door = reg(id + "_door", p -> new DoorBlock(BlockSetType.OAK, p), wood(planksColor, 3.0f, 3.0f).noOcclusion().pushReaction(PushReaction.DESTROY));
        s.trapdoor = reg(id + "_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), wood(planksColor, 3.0f, 3.0f).noOcclusion().isValidSpawn((st, lv, pos, e) -> false));
        s.button = reg(id + "_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p),
                BlockBehaviour.Properties.of().noCollission().strength(0.5f).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY));
        s.plate = reg(id + "_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p),
                BlockBehaviour.Properties.of().mapColor(planksColor).forceSolidOn().noCollission().strength(0.5f).sound(SoundType.WOOD).ignitedByLava().pushReaction(PushReaction.DESTROY));

        item(s, "stripped_" + id + "_log", s.strippedLog); item(s, id + "_log", s.log); item(s, "thin_" + id + "_log", s.thinLog);
        item(s, "stripped_" + id + "_wood", s.strippedWood); item(s, id + "_wood", s.wood);
        item(s, id + "_leaves", s.leaves); item(s, id + "_sapling", s.sapling); item(s, id + "_planks", s.planks);
        item(s, id + "_stairs", s.stairs); item(s, id + "_slab", s.slab); item(s, id + "_fence", s.fence); item(s, id + "_fence_gate", s.gate);
        s.items.add(ModItems.ITEMS.registerItem(id + "_door", p -> new DoubleHighBlockItem(s.door.get(), p)));
        item(s, id + "_trapdoor", s.trapdoor); item(s, id + "_button", s.button); item(s, id + "_pressure_plate", s.plate);
        SETS.add(s);
    }

    /**
     * Вид, у которого своя только листва и саженец, а брёвна, доски и всё остальное — от вида baseId.
     * Его деревья (configured_feature <id>_tree) растут из своего саженца, ствол — из брёвен baseId.
     */
    private static void addLeafVariant(String id, String baseId) {
        WoodSet base = SETS.stream().filter(b -> b.id.equals(baseId)).findFirst().orElseThrow();
        WoodSet s = new WoodSet(id);
        s.log = base.log; s.thinLog = base.thinLog; s.strippedLog = base.strippedLog; s.wood = base.wood;
        s.strippedWood = base.strippedWood; s.planks = base.planks; s.stairs = base.stairs; s.slab = base.slab;
        s.fence = base.fence; s.gate = base.gate; s.door = base.door; s.trapdoor = base.trapdoor;
        s.button = base.button; s.plate = base.plate;
        s.leaves = leaves(id);
        s.sapling = sapling(id);
        item(s, id + "_leaves", s.leaves); item(s, id + "_sapling", s.sapling);
        SETS.add(s);
    }

    private static DeferredBlock<Block> leaves(String id) {
        return reg(id + "_leaves", LeavesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks()
                .sound(SoundType.GRASS).noOcclusion().isValidSpawn((st, lv, pos, e) -> false).isSuffocating((st, lv, pos) -> false)
                .isViewBlocking((st, lv, pos) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY));
    }

    /** Саженец растит дерево configured_feature trailcraft:<id>_tree. */
    private static DeferredBlock<Block> sapling(String id) {
        ResourceKey<ConfiguredFeature<?, ?>> treeKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, id + "_tree"));
        TreeGrower grower = new TreeGrower(TrailCraft.MODID + ":" + id, Optional.empty(), Optional.of(treeKey), Optional.empty());
        return reg(id + "_sapling", p -> new SaplingBlock(grower, p), BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission()
                .randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY));
    }

    private static void item(WoodSet s, String name, DeferredBlock<Block> block) {
        s.items.add(ModItems.ITEMS.registerSimpleBlockItem(name, block));
    }
}

package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    /** Породы, о которые трут ветку, чтобы получить обработанную палку. */
    public static final TagKey<Block> ROCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "rocks"));

    /** Блоки, на которых может расти трава (grass_small/medium/tall). Список: data/trailcraft/tags/block/grass_plantable.json */
    public static final TagKey<Block> GRASS_PLANTABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "grass_plantable"));

    /** Верстаки пород (scripts/gen_workbench.py). */
    public static final TagKey<Item> WORKBENCHES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "workbenches"));
    /** Тонкие брёвна: одно такое бревно и 4 ветки, разожжённые огнивом, дают костёр. */
    public static final TagKey<Item> THIN_LOGS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "thin_logs"));
    /** Тонкие брёвна — не в #minecraft:logs, а в своём тэге; листва рядом с ними не опадает (mixin/LeavesBlockMixin). */
    public static final TagKey<Block> THIN_LOG_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "thin_logs"));

    /** Слитки бронзы (оловянной и висмутовой): из них бронзовые инструменты. */
    public static final TagKey<Item> BRONZE_INGOTS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "ingots/bronze"));

    /** Топливо малой сыродутной печи: древесный и каменный уголь (#minecraft:coals) и торф. */
    public static final TagKey<Item> BLOOMERY_FUELS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "bloomery_fuels"));

    /** Обваливаются, если под ними пусто и рядом нет крепи (collapse/Collapse): породы и руды. */
    public static final TagKey<Block> CAN_COLLAPSE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "can_collapse"));
    /** Почвы осыпаются, как песок, когда игрок выкопал блок под ними и рядом нет крепи (collapse/Collapse). */
    public static final TagKey<Block> COLLAPSIBLE_SOILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "collapsible_soils"));
    /** Хвойные брёвна: при окорке с них, кроме коры, падает смола (StrippableLogBlock). */
    public static final TagKey<Block> RESIN_LOGS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "resin_logs"));
    /** На чём растёт наш кактус (наш песок). */
    public static final TagKey<Block> CACTUS_SOILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "cactus_soils"));
    /** На чём растёт наш сахарный тростник (почвы и песок у воды). */
    public static final TagKey<Block> CANE_SOILS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "cane_soils"));
    /** Брёвна, на которых растёт какао (пальма, эбеновое дерево). */
    public static final TagKey<Block> COCOA_LOGS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "cocoa_logs"));
    /** Блоки угля (топливо угольной горелки — как 9 углей). */
    public static final TagKey<Item> COAL_BLOCKS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/coal"));
}

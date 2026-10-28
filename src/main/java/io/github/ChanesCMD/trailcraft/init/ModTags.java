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
}

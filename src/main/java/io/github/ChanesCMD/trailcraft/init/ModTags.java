package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    /** Породы, о которые трут ветку, чтобы получить обработанную палку. */
    public static final TagKey<Block> ROCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "rocks"));

    /** Блоки, на которых может расти трава (grass_small/medium/tall). Список: data/trailcraft/tags/block/grass_plantable.json */
    public static final TagKey<Block> GRASS_PLANTABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "grass_plantable"));
}

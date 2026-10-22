package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

public final class ModTiers {
    /**
     * Что медь не добывает: то же, что и камень (железная руда и выше), но без брёвен,
     * поэтому медный топор рубит любые брёвна, а каменный/кремнёвый — только тонкие.
     */
    public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "incorrect_for_copper_tool"));

    /** Медь: между камнем и железом (прочность 190, скорость 5). */
    public static final Tier COPPER = new SimpleTier(INCORRECT_FOR_COPPER_TOOL, 190, 5.0f, 1.0f, 5,
            () -> Ingredient.of(Items.COPPER_INGOT));

    private ModTiers() {}
}

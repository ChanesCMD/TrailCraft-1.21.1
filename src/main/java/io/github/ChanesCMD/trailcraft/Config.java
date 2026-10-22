package io.github.ChanesCMD.trailcraft;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Настройки мода (файл config/trailcraft-common.toml). */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REMOVE_VANILLA_CRAFTING = BUILDER
            .comment("Remove all vanilla (minecraft:) recipes of the crafting table, smithing table and stonecutter on every datapack reload.",
                    "Smelting, blasting, smoking and campfire recipes are kept so ores can still be cooked.")
            .define("removeVanillaCraftingRecipes", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> KEEP_VANILLA_RECIPES = BUILDER
            .comment("Vanilla recipe ids that stay even when removal is on, for example \"minecraft:torch\".")
            .defineListAllowEmpty("keepVanillaRecipes", List.of(), () -> "minecraft:torch", Config::validateRecipeId);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateRecipeId(final Object obj) {
        return obj instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}

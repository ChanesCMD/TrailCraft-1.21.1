package io.github.ChanesCMD.trailcraft.compat.jei;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.init.ModWood;
import io.github.ChanesCMD.trailcraft.item.BranchStickItem;
import io.github.ChanesCMD.trailcraft.item.ClothBlankItem;
import io.github.ChanesCMD.trailcraft.smelting.Smelting;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import io.github.ChanesCMD.trailcraft.smelting.StrawEvents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * JEI (только если он установлен): плавка в кувшине в малой сыродутной печи и «ручная работа» — всё, что делается
 * не в сетке крафта (обработка ветки о породу, ткачество, слепок, солома, ветки с листвы, пень для рубки, костёр).
 */
@JeiPlugin
public class TrailJeiPlugin implements IModPlugin {
    static final RecipeType<TrailJeiRecipe> BLOOMERY = RecipeType.create(TrailCraft.MODID, "small_bloomery", TrailJeiRecipe.class);
    static final RecipeType<TrailJeiRecipe> HANDWORK = RecipeType.create(TrailCraft.MODID, "handwork", TrailJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new TrailJeiCategory(gui, BLOOMERY, Component.translatable("jei.trailcraft.small_bloomery"),
                        new ItemStack(ModSmelting.SMALL_BLOOMERY_ITEM.get())),
                new TrailJeiCategory(gui, HANDWORK, Component.translatable("jei.trailcraft.handwork"),
                        new ItemStack(ModItems.STICK_PROCESSED.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModSmelting.SMALL_BLOOMERY_ITEM.get()), BLOOMERY);
        registration.addRecipeCatalyst(new ItemStack(ModSmelting.JUG.get()), BLOOMERY);
        // верстаки пород — то же окно крафта, что у ванильного
        for (ModWood.WoodSet set : ModWood.SETS) {
            if (set.craftingTable != null) {
                registration.addRecipeCatalyst(new ItemStack(set.craftingTable.get()), RecipeTypes.CRAFTING);
            }
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BLOOMERY, bloomery());
        registration.addRecipes(HANDWORK, handwork());
    }

    private static List<TrailJeiRecipe> bloomery() {
        List<TrailJeiRecipe> recipes = new ArrayList<>();
        Component one = Component.translatable("jei.trailcraft.bloomery.one");
        for (Metal metal : List.of(Metal.COPPER, Metal.TIN, Metal.ZINC, Metal.BISMUTH, Metal.LEAD, Metal.GOLD)) {
            recipes.add(new TrailJeiRecipe(List.of(stacks(Smelting.sources(metal), 1)), new ItemStack(metal.ingot()), one));
        }
        Component alloy = Component.translatable("jei.trailcraft.bloomery.alloy");
        List<Item> copper = Smelting.sources(Metal.COPPER);
        recipes.add(new TrailJeiRecipe(List.of(stacks(copper, 3), stacks(Smelting.sources(Metal.TIN), 1)),
                new ItemStack(Metal.TIN_BRONZE.ingot(), 4), alloy));
        recipes.add(new TrailJeiRecipe(List.of(stacks(copper, 3), stacks(Smelting.sources(Metal.ZINC), 1)),
                new ItemStack(Metal.BRASS.ingot(), 4), alloy));
        recipes.add(new TrailJeiRecipe(List.of(stacks(copper, 2), stacks(Smelting.sources(Metal.BISMUTH), 1),
                stacks(Smelting.sources(Metal.ZINC), 1)), new ItemStack(Metal.BISMUTH_BRONZE.ingot(), 4), alloy));
        // шихта сплавов плавится сразу в сплав
        Component mix = Component.translatable("jei.trailcraft.bloomery.mix");
        for (String[] pair : new String[][]{{"tin_bronze_mix", "tin_bronze_ingot"}, {"bismuth_bronze_mix", "bismuth_bronze_ingot"}}) {
            recipes.add(new TrailJeiRecipe(List.of(List.of(stack(TrailCraft.MODID + ":" + pair[0]))),
                    stack(TrailCraft.MODID + ":" + pair[1]), mix));
        }
        recipes.add(new TrailJeiRecipe(List.of(List.of(stack(TrailCraft.MODID + ":brass_mix"))),
                new ItemStack(Metal.BRASS.ingot()), mix));
        return recipes;
    }

    private static List<TrailJeiRecipe> handwork() {
        List<TrailJeiRecipe> recipes = new ArrayList<>();
        List<ItemStack> rocks = List.of("andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite",
                "gray_granite", "limestone", "marble", "sandstone", "shale").stream()
                .map(r -> stack(TrailCraft.MODID + ":" + r)).filter(s -> !s.isEmpty()).toList();
        ItemStack branch = new ItemStack(ModItems.STICK_BRANCH.get());
        ItemStack stick = new ItemStack(ModItems.STICK_PROCESSED.get());
        recipes.add(new TrailJeiRecipe(List.of(List.of(branch), rocks), stick,
                Component.translatable("jei.trailcraft.branch", BranchStickItem.RUBS_NEEDED)));
        recipes.add(new TrailJeiRecipe(List.of(List.of(new ItemStack(ModItems.CLOTH_BLANK.get())), List.of(stick)),
                new ItemStack(ModItems.CLOTH.get()),
                Component.translatable("jei.trailcraft.cloth", Math.round(ClothBlankItem.STICK_RETURN * 100))));
        recipes.add(new TrailJeiRecipe(List.of(List.of(new ItemStack(ModSmelting.CLAY_MOLD.get()))),
                new ItemStack(ModSmelting.UNFIRED_JUG.get()), Component.translatable("jei.trailcraft.clay_mold")));
        List<ItemStack> grass = List.of(TrailCraft.MODID + ":grass_small", TrailCraft.MODID + ":grass_medium",
                TrailCraft.MODID + ":grass_tall", "minecraft:short_grass", "minecraft:tall_grass").stream()
                .map(TrailJeiPlugin::stack).filter(s -> !s.isEmpty()).toList();
        recipes.add(new TrailJeiRecipe(List.of(grass, tag(StrawEvents.STRAW_TOOLS)), new ItemStack(ModSmelting.STRAW.get()),
                Component.translatable("jei.trailcraft.straw")));
        List<ItemStack> leaves = items(id -> id.getNamespace().equals(TrailCraft.MODID) && id.getPath().endsWith("_leaves"));
        recipes.add(new TrailJeiRecipe(List.of(leaves), branch, Component.translatable("jei.trailcraft.leaves_branch")));
        List<ItemStack> logs = items(id -> id.getNamespace().equals(TrailCraft.MODID) && id.getPath().endsWith("_log")
                && !id.getPath().startsWith("thin_"));
        List<ItemStack> thinLogs = items(id -> id.getNamespace().equals(TrailCraft.MODID) && id.getPath().startsWith("thin_")
                && id.getPath().endsWith("_log"));
        recipes.add(new TrailJeiRecipe(List.of(logs, List.of(new ItemStack(ModBlocks.CHOPPING_STUMP.get())),
                tag(ItemTags.AXES)), thinLogs.isEmpty() ? ItemStack.EMPTY : thinLogs.get(0).copyWithCount(2),
                Component.translatable("jei.trailcraft.chopping")));
        recipes.add(new TrailJeiRecipe(List.of(thinLogs, List.of(branch.copyWithCount(4)),
                List.of(new ItemStack(ModItems.FIRESTARTER.get()))), new ItemStack(Items.CAMPFIRE),
                Component.translatable("jei.trailcraft.campfire")));
        return recipes;
    }

    private static List<ItemStack> stacks(List<Item> items, int count) {
        return items.stream().map(i -> new ItemStack(i, count)).toList();
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static List<ItemStack> tag(net.minecraft.tags.TagKey<Item> tag) {
        return Arrays.asList(Ingredient.of(tag).getItems());
    }

    private static List<ItemStack> items(Predicate<ResourceLocation> filter) {
        List<ItemStack> out = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (filter.test(id)) {
                out.add(new ItemStack(BuiltInRegistries.ITEM.get(id)));
            }
        }
        out.sort((a, b) -> BuiltInRegistries.ITEM.getKey(a.getItem()).compareTo(BuiltInRegistries.ITEM.getKey(b.getItem())));
        return out;
    }
}

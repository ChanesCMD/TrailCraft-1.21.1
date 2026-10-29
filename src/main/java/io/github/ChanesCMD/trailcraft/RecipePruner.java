package io.github.ChanesCMD.trailcraft;

import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Убирает все ванильные рецепты (namespace minecraft): верстака, кузнечного стола, камнерезки, печи, доменной печи,
 * коптильни и костра — с ними уходит и массовая плавка Create вентилятором (брони в самородки и т. п.); еда на костре и
 * в коптильне, красители — свои копии (recipe/food/, recipe/dyes/). Ещё убирает рецепты по списку {@link #LIST} (data/trailcraft/pruned_recipes.json, пишет
 * scripts/gen_create_compat.py): рецепты Create из ванильных предметов, которых в мире нет, и рецепты Create, вместо
 * которых есть свои. Наши слушатель стоит в конце списка, поэтому срабатывает после загрузки рецептов.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class RecipePruner {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        RecipeManager manager = event.getServerResources().getRecipeManager();
        event.addListener((barrier, resourceManager, prepProfiler, applyProfiler, backgroundExecutor, gameExecutor) ->
                barrier.wait(Unit.INSTANCE).thenRunAsync(() -> prune(manager, listed(resourceManager)), gameExecutor));
    }

    /** Список рецептов, которые убрать, — массив id в data/trailcraft/pruned_recipes.json. */
    public static final ResourceLocation LIST = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "pruned_recipes.json");

    private static Set<String> listed(ResourceManager resources) {
        Set<String> ids = new HashSet<>();
        for (Resource resource : resources.getResourceStack(LIST)) {
            try (Reader reader = resource.openAsReader()) {
                JsonParser.parseReader(reader).getAsJsonArray().forEach(id -> ids.add(id.getAsString()));
            } catch (Exception e) {
                TrailCraft.LOGGER.error("TrailCraft: can't read {}", LIST, e);
            }
        }
        return ids;
    }

    private static void prune(RecipeManager manager, Set<String> listed) {
        boolean enabled;
        Set<String> keep = new HashSet<>();
        try {
            enabled = Config.REMOVE_VANILLA_CRAFTING.get();
            keep.addAll(Config.KEEP_VANILLA_RECIPES.get());
        } catch (IllegalStateException configNotLoaded) {
            enabled = true;
        }
        if (!enabled) return;

        List<RecipeHolder<?>> kept = new ArrayList<>();
        int removed = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            String id = holder.id().toString();
            if ((isRemovable(holder) || listed.contains(id)) && !keep.contains(id)) {
                removed++;
            } else {
                kept.add(holder);
            }
        }
        manager.replaceRecipes(kept);
        TrailCraft.LOGGER.info("TrailCraft: removed {} vanilla and listed recipes, {} left", removed, kept.size());
    }

    private static boolean isRemovable(RecipeHolder<?> holder) {
        if (!"minecraft".equals(holder.id().getNamespace())) return false;
        RecipeType<?> type = holder.value().getType();
        return type == RecipeType.CRAFTING || type == RecipeType.SMITHING || type == RecipeType.STONECUTTING
                || type == RecipeType.SMELTING || type == RecipeType.BLASTING || type == RecipeType.SMOKING
                || type == RecipeType.CAMPFIRE_COOKING;
    }
}

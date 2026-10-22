package io.github.ChanesCMD.trailcraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Убирает ванильные рецепты верстака, кузнечного стола и камнерезки (namespace minecraft).
 * Плавка, доменная печь, коптильня и костёр остаются, чтобы можно было переплавлять руды.
 * Наши слушатель стоит в конце списка, поэтому срабатывает после загрузки рецептов.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class RecipePruner {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        RecipeManager manager = event.getServerResources().getRecipeManager();
        event.addListener((barrier, resourceManager, prepProfiler, applyProfiler, backgroundExecutor, gameExecutor) ->
                barrier.wait(Unit.INSTANCE).thenRunAsync(() -> prune(manager), gameExecutor));
    }

    private static void prune(RecipeManager manager) {
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
            if (isRemovable(holder) && !keep.contains(holder.id().toString())) {
                removed++;
            } else {
                kept.add(holder);
            }
        }
        manager.replaceRecipes(kept);
        TrailCraft.LOGGER.info("TrailCraft: removed {} vanilla recipes, {} left", removed, kept.size());
    }

    private static boolean isRemovable(RecipeHolder<?> holder) {
        if (!"minecraft".equals(holder.id().getNamespace())) return false;
        RecipeType<?> type = holder.value().getType();
        return type == RecipeType.CRAFTING || type == RecipeType.SMITHING || type == RecipeType.STONECUTTING;
    }
}

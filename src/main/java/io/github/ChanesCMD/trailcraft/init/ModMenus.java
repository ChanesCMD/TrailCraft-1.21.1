package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TrailCraft.MODID);

    /** Инвентарь игрока (заменяет ванильный по клавише E, кроме креатива). */
    public static final DeferredHolder<MenuType<?>, MenuType<TrailInventoryMenu>> INVENTORY =
            MENUS.register("inventory", () -> new MenuType<>(TrailInventoryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}

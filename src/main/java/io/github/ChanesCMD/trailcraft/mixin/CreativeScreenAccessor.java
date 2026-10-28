package io.github.ChanesCMD.trailcraft.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Выбранная вкладка креативного окна: вкладки над инвентарём TrailCraft открывают креатив сразу на нужной (CreativeTabs). */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenAccessor {
    @Accessor("selectedTab")
    static void trailcraft$setSelectedTab(CreativeModeTab tab) {
        throw new AssertionError();
    }
}

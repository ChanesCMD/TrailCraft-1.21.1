package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Инструмент из плохо оббитой головки выходит уже потрёпанным (на 40 % прочности меньше). */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HeadQuality {
    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!result.isDamageableItem()) {
            return;
        }
        Container inventory = event.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty() || slot.getOrDefault(ModComponents.QUALITY.get(), 0) <= 0) {
                continue;
            }
            int max = result.getMaxDamage();
            result.set(DataComponents.DAMAGE, Math.max(1, Math.round(max * 0.4f)));
            return;
        }
    }

    private HeadQuality() {}
}

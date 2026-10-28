package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.TrailCreativeScreen;
import io.github.ChanesCMD.trailcraft.inventory.OpenInventoryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Вместо ванильного креативного окна — своё (TrailCreativeScreen, размером с инвентарь TrailCraft), на последней
 * выбранной вкладке; если ей была «Инвентарь» — сразу инвентарь TrailCraft (его открывает сервер).
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientCreativeEvents {
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        var player = Minecraft.getInstance().player;
        // в креативе E открывает ванильный InventoryScreen, а тот сразу — креативное окно
        boolean creativeInventory = event.getNewScreen() instanceof InventoryScreen && player != null && player.isCreative();
        if (!(event.getNewScreen() instanceof CreativeModeInventoryScreen || creativeInventory) || player == null) {
            return;
        }
        CreativeModeTab last = TrailCreativeScreen.lastTab();
        if (last != null && last.getType() == CreativeModeTab.Type.INVENTORY) {
            event.setCanceled(true);
            // ванильное окно уже подменило меню игрока своим — вернуть, сервер откроет инвентарь TrailCraft
            player.containerMenu = player.inventoryMenu;
            PacketDistributor.sendToServer(OpenInventoryPayload.fromCreative(ItemStack.EMPTY));
        } else if (event.getNewScreen() instanceof CreativeModeInventoryScreen) {
            event.setNewScreen(new TrailCreativeScreen(player, last));
        }
    }
}

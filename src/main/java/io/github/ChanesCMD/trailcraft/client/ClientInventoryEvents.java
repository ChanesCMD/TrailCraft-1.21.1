package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.TrailInventoryScreen;
import io.github.ChanesCMD.trailcraft.client.gui.BloomeryScreen;
import io.github.ChanesCMD.trailcraft.client.gui.JugScreen;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.inventory.DeathLogPayloads;
import io.github.ChanesCMD.trailcraft.inventory.OpenInventoryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientInventoryEvents {

    /** Вместо ванильного инвентаря просим сервер открыть свой. В креативе — креативное окно (его вкладка «Инвентарь» — свой, ClientCreativeEvents). */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        var player = Minecraft.getInstance().player;
        if (event.getNewScreen() instanceof InventoryScreen && player != null && !player.isCreative()) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(OpenInventoryPayload.INSTANCE);
        }
    }

    /** Сервер прислал журнал смертей — показываем его в открытом инвентаре. */
    public static void showDeathLog(DeathLogPayloads.Log log) {
        if (Minecraft.getInstance().screen instanceof TrailInventoryScreen screen) {
            screen.showDeathLog(log);
        }
    }

    /** Сервер прислал заметки календаря — показываем их в открытом инвентаре. */
    public static void showCalendarNotes(Map<String, String> notes) {
        if (Minecraft.getInstance().screen instanceof TrailInventoryScreen screen) {
            screen.showCalendarNotes(notes);
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.INVENTORY.get(), TrailInventoryScreen::new);
            event.register(ModSmelting.JUG_MENU.get(), JugScreen::new);
            event.register(ModSmelting.BLOOMERY_MENU.get(), BloomeryScreen::new);
            event.register(ModMenus.BARREL.get(), io.github.ChanesCMD.trailcraft.client.gui.BarrelScreen::new);
            event.register(io.github.ChanesCMD.trailcraft.init.ModBees.APIARY_MENU.get(), io.github.ChanesCMD.trailcraft.client.gui.ApiaryScreen::new);
        }
    }
}

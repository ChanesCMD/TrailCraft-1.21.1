package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.inventory.OpenInventoryPayload;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Креативная вкладка «Инвентарь» — это инвентарь TrailCraft, как в выживании, с вкладками креатива сверху
 * (client/gui/CreativeTabs): выбрали вкладку (или креативное окно открылось на ней) — просим сервер открыть инвентарь
 * TrailCraft; предмет на курсоре остаётся на курсоре.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientCreativeEvents {
    /** Запрос уже отправлен из этого окна (если сервер не открыл инвентарь, не шлём его каждый кадр). */
    private static CreativeModeInventoryScreen requested;

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        if (!screen.isInventoryOpen()) {
            requested = null;
            return;
        }
        if (requested != screen) {
            requested = screen;
            ItemStack carried = screen.getMenu().getCarried().copy();
            screen.getMenu().setCarried(ItemStack.EMPTY);
            PacketDistributor.sendToServer(OpenInventoryPayload.fromCreative(carried));
            event.setCanceled(true);
        }
    }
}

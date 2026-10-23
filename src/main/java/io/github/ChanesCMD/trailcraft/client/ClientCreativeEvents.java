package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * Креативная вкладка «Инвентарь»: ванильные слоты 9..35 моду не нужны (основной инвентарь лежит в рюкзаке),
 * поэтому прячем их за экран, как ванилла прячет слоты крафта, и закрашиваем их сетку на фоне.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientCreativeEvents {
    /** Номера слотов меню инвентаря игрока, которые прячем, и где их сетка на фоне вкладки. */
    private static final int FIRST = 9, LAST = 35, GRID_X = 8, GRID_Y = 53, GRID_W = 9 * 18, GRID_H = 3 * 18;
    private static final int HIDDEN = -2000;

    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof CreativeModeInventoryScreen screen) || !screen.isInventoryOpen()) {
            return;
        }
        // Вкладку могли только что выбрать — слоты создаются заново, поэтому проверяем каждый кадр.
        // Обёртки вкладки добавлены в список напрямую (index не задан), номер слота меню — getContainerSlot().
        // Координаты слота неизменяемы, поэтому меняем обёртку на обычный слот за экраном: креатив обращается
        // к своей обёртке только при клике, а по слоту за экраном кликнуть нельзя
        List<Slot> slots = screen.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            int menuSlot = slot.getContainerSlot();
            if (menuSlot >= FIRST && menuSlot <= LAST && slot.container instanceof Inventory && slot.x != HIDDEN) {
                slots.set(i, new Slot(slot.container, menuSlot, HIDDEN, HIDDEN));
            }
        }
        int x = screen.getGuiLeft() + GRID_X, y = screen.getGuiTop() + GRID_Y;
        event.getGuiGraphics().fill(x, y, x + GRID_W, y + GRID_H, UiDraw.PANEL);
    }

    /** Shift-клик во вкладке «Инвентарь» переложил бы предмет в спрятанные слоты — отключаем его там. */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen screen && screen.isInventoryOpen() && Screen.hasShiftDown()
                && screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().container instanceof Inventory) {
            event.setCanceled(true);
        }
    }
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import io.github.ChanesCMD.trailcraft.inventory.EquippedPayloads;
import io.github.ChanesCMD.trailcraft.inventory.OpenInventoryPayload;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import io.github.ChanesCMD.trailcraft.inventory.TrailInventoryMenu.Popup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Креативная вкладка «Инвентарь»: ванильные слоты 9..35 моду не нужны (основной инвентарь лежит в рюкзаке),
 * поэтому прячем их за экран, как ванилла прячет слоты крафта, и закрашиваем их сетку на фоне.
 * На освободившемся месте — слоты кошелька, пояса и рюкзака (их содержимое присылает сервер, см. EquippedPayloads);
 * плюс у слота открывает инвентарь TrailCraft сразу с окном этого предмета.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientCreativeEvents {
    /** Номера слотов меню инвентаря игрока, которые прячем, и где их сетка на фоне вкладки. */
    private static final int FIRST = 9, LAST = 35, GRID_X = 8, GRID_Y = 53, GRID_W = 9 * 18, GRID_H = 3 * 18;
    private static final int HIDDEN = -2000;
    /** Рамка первого слота снаряжения (кошелёк, пояс, рюкзак) относительно вкладки; подпись над ними. */
    private static final int GEAR_X = GRID_X, GEAR_Y = GRID_Y + 18, LABEL_Y = GRID_Y + 6;
    /** Плюс открытия окна — в правом верхнем углу слота, как в инвентаре TrailCraft. */
    private static final int PLUS_DX = 10, PLUS_DY = 1;
    /** Окна слотов EquippedPayloads.SLOTS: кошелёк, пояс, рюкзак. */
    private static final Popup[] POPUPS = {Popup.WALLET, Popup.BELT, Popup.BACKPACK};

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
        GuiGraphics g = event.getGuiGraphics();
        int x = screen.getGuiLeft() + GRID_X, y = screen.getGuiTop() + GRID_Y;
        g.fill(x, y, x + GRID_W, y + GRID_H, UiDraw.PANEL);
        renderGear(g, screen, event.getMouseX(), event.getMouseY());
    }

    /** Слоты кошелька, пояса и рюкзака: рамка, предмет, подсветка под курсором. */
    private static void renderGear(GuiGraphics g, CreativeModeInventoryScreen screen, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int left = screen.getGuiLeft(), top = screen.getGuiTop();
        g.drawString(font, Component.translatable("gui.trailcraft.creative.gear"), left + GEAR_X, top + LABEL_Y, UiDraw.LABEL, false);
        for (int k = 0; k < EquippedPayloads.SLOTS.length; k++) {
            int sx = left + GEAR_X + 18 * k, sy = top + GEAR_Y;
            UiDraw.slot(g, sx, sy);
            ItemStack stack = ClientGear.equipped(k);
            g.renderItem(stack, sx + 1, sy + 1);
            g.renderItemDecorations(font, stack, sx + 1, sy + 1);
            if (gearSlotAt(screen, mouseX, mouseY) == k && plusAt(screen, mouseX, mouseY) < 0) {
                g.fill(RenderType.guiOverlay(), sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF);
            }
            // Плюс открывает окно предмета, как в инвентаре TrailCraft; поверх предмета в слоте
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            UiDraw.plusButton(g, sx + PLUS_DX, sy + PLUS_DY);
            g.pose().popPose();
        }
    }

    /** Подсказка к предмету в слоте снаряжения или к пустому слоту — поверх всего экрана. */
    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen) || !screen.isInventoryOpen()
                || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        int k = gearSlotAt(screen, event.getMouseX(), event.getMouseY());
        if (k < 0 || plusAt(screen, event.getMouseX(), event.getMouseY()) >= 0) {
            return;
        }
        var font = Minecraft.getInstance().font;
        ItemStack stack = ClientGear.equipped(k);
        if (stack.isEmpty()) {
            event.getGuiGraphics().renderTooltip(font, Component.translatable(EMPTY_HINTS[k]), event.getMouseX(), event.getMouseY());
        } else {
            event.getGuiGraphics().renderTooltip(font, stack, event.getMouseX(), event.getMouseY());
        }
    }

    /** Подписи пустых слотов — названия предметов, которые туда кладутся. */
    private static final String[] EMPTY_HINTS = {"item.trailcraft.wallet", "item.trailcraft.belt", "gui.trailcraft.backpack"};

    /**
     * Клик по слоту снаряжения: положить предмет с курсора (если подходит), забрать или обменять. Сервер получает
     * новый предмет слота (EquippedPayloads.Set), как ванильный креатив — содержимое своих слотов.
     * Shift-клик во вкладке «Инвентарь» переложил бы предмет в спрятанные слоты — отключаем его там.
     */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen) || !screen.isInventoryOpen()) {
            return;
        }
        int plus = plusAt(screen, event.getMouseX(), event.getMouseY());
        if (plus >= 0 && screen.getMenu().getCarried().isEmpty()) {
            // Окна кошелька, пояса и рюкзака есть только в инвентаре TrailCraft: открываем его с нужным окном
            event.setCanceled(true);
            ClientInventoryEvents.pendingPopup = POPUPS[plus];
            PacketDistributor.sendToServer(OpenInventoryPayload.FROM_CREATIVE);
            return;
        }
        int k = gearSlotAt(screen, event.getMouseX(), event.getMouseY());
        if (k >= 0) {
            event.setCanceled(true);
            clickGear(screen.getMenu(), k);
            return;
        }
        if (Screen.hasShiftDown() && screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().container instanceof Inventory) {
            event.setCanceled(true);
        }
    }

    private static void clickGear(AbstractContainerMenu menu, int k) {
        int slot = EquippedPayloads.SLOTS[k];
        ItemStack carried = menu.getCarried(), current = ClientGear.equipped(k);
        if (!carried.isEmpty() && (!fits(slot, carried) || carried.getCount() > 1 && !current.isEmpty())) {
            return;
        }
        if (carried.isEmpty() && current.isEmpty()) {
            return;
        }
        ItemStack placed = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        ItemStack rest = carried.getCount() > 1 ? carried.copyWithCount(carried.getCount() - 1) : current;
        ClientGear.setEquipped(k, placed);
        menu.setCarried(rest);
        PacketDistributor.sendToServer(new EquippedPayloads.Set(slot, placed));
    }

    private static boolean fits(int slot, ItemStack stack) {
        return switch (slot) {
            case PlayerSlots.WALLET -> stack.is(PlayerSlots.WALLETS);
            case PlayerSlots.BELT -> stack.is(PlayerSlots.BELTS);
            default -> stack.is(PlayerSlots.BACKPACKS);
        };
    }

    /** Номер слота, чей плюс под курсором, или -1. */
    private static int plusAt(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        for (int k = 0; k < EquippedPayloads.SLOTS.length; k++) {
            double x = mouseX - (screen.getGuiLeft() + GEAR_X + 18 * k + PLUS_DX);
            double y = mouseY - (screen.getGuiTop() + GEAR_Y + PLUS_DY);
            if (x >= 0 && x < 7 && y >= 0 && y < 7) {
                return k;
            }
        }
        return -1;
    }

    /** Номер слота снаряжения под курсором (0 — кошелёк, 1 — пояс, 2 — рюкзак) или -1. */
    private static int gearSlotAt(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        double x = mouseX - screen.getGuiLeft() - GEAR_X, y = mouseY - screen.getGuiTop() - GEAR_Y;
        if (x < 0 || y < 0 || y >= 18 || x >= 18 * EquippedPayloads.SLOTS.length) {
            return -1;
        }
        return (int) x / 18;
    }
}

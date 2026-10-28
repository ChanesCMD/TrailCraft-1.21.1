package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.JugMenu;
import io.github.ChanesCMD.trailcraft.smelting.Molten;
import io.github.ChanesCMD.trailcraft.smelting.Smelting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Окно кувшина: 4 слота, ниже — что выплавится в малой сыродутной печи (или жидкий металл в кувшине) и слот формы. */
public class JugScreen extends AbstractContainerScreen<JugMenu> {
    public JugScreen(JugMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = JugMenu.WIDTH;
        imageHeight = JugMenu.HEIGHT;
        inventoryLabelY = JugMenu.HOTBAR_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        UiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            UiDraw.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int y = JugMenu.JUG_Y + 22;
        Molten molten = menu.molten();
        List<ItemStack> items = menu.jugItems();
        if (molten != null) {
            centered(g, molten.describe(), y, 0xFFB04A00);
        } else if (items.stream().anyMatch(Smelting::melts)) {
            Optional<Molten> result = Smelting.melt(items);
            Component text = result.isPresent()
                    ? Component.translatable("tooltip.trailcraft.jug.result", result.get().describe())
                    : Component.translatable(Smelting.hasForeign(items) ? "tooltip.trailcraft.jug.foreign" : "tooltip.trailcraft.jug.no_alloy");
            centered(g, text, y, 0xFF8A5A00);
        }
        Component label = Component.translatable("gui.trailcraft.jug.mold");
        g.drawString(font, label, JugMenu.MOLD_X - 4 - font.width(label), JugMenu.MOLD_Y + 4, UiDraw.LABEL, false);
    }

    /** Строка по центру окна; не влезает (длинные названия сплавов) — мельче. */
    private void centered(GuiGraphics g, Component text, int y, int color) {
        int width = font.width(text), room = imageWidth - 8;
        float scale = width > room ? (float) room / width : 1;
        g.pose().pushPose();
        g.pose().translate((imageWidth - width * scale) / 2, y + (1 - scale) * 4, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }
}

package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.JugMenu;
import io.github.ChanesCMD.trailcraft.smelting.Smelting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Окно кувшина: 4 слота, ниже — что выплавится в малой сыродутной печи. */
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
        List<ItemStack> result = Smelting.result(menu.jugItems());
        int y = JugMenu.JUG_Y + 22;
        if (Smelting.hasForeign(menu.jugItems()) && menu.jugItems().stream().anyMatch(Smelting::melts)) {
            centered(g, Component.translatable("tooltip.trailcraft.jug.foreign"), y, 0xFF8A5A00);
        }
        for (ItemStack ingot : result) {
            centered(g, Component.translatable("tooltip.trailcraft.jug.result", ingot.getCount(), ingot.getHoverName()), y, 0xFF8A5A00);
            y += 10;
        }
    }

    private void centered(GuiGraphics g, Component text, int y, int color) {
        g.drawString(font, text, (imageWidth - font.width(text)) / 2, y, color, false);
    }
}

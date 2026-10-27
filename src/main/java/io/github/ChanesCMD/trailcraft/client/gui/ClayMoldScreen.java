package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.smelting.ClayMoldItem;
import io.github.ChanesCMD.trailcraft.smelting.ClayShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Окно слепка: плитки с формами (ClayShape), клик — вылепить. */
public class ClayMoldScreen extends Screen {
    private static final int TILE = 26, PAD = 8, WIDTH = 140;

    public ClayMoldScreen() {
        super(Component.translatable("gui.trailcraft.clay_mold"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ClayMoldScreen());
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - height()) / 2;
    }

    private int height() {
        int rows = (ClayShape.values().length + 3) / 4;
        return 22 + rows * (TILE + 4) + 18;
    }

    private int tileX(int i) {
        return left() + PAD + (i % 4) * (TILE + 6);
    }

    private int tileY(int i) {
        return top() + 20 + (i / 4) * (TILE + 4);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        UiDraw.panel(g, left(), top(), WIDTH, height());
        g.drawString(font, title, left() + PAD, top() + 7, UiDraw.LABEL, false);
        ClayShape[] shapes = ClayShape.values();
        for (int i = 0; i < shapes.length; i++) {
            int x = tileX(i), y = tileY(i);
            boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
            UiDraw.frame(g, x, y, TILE, TILE, 0xFF373737, 0xFFFFFFFF, hover ? 0xFFB5A58A : UiDraw.GREY);
            ItemStack icon = new ItemStack(shapes[i].result());
            g.renderItem(icon, x + 5, y + 5);
            if (hover) {
                g.renderTooltip(font, icon.getHoverName(), mouseX, mouseY);
            }
        }
        g.drawString(font, Component.translatable("gui.trailcraft.clay_mold.hint"), left() + PAD,
                top() + height() - 14, 0xFF606060, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ClayShape[] shapes = ClayShape.values();
        for (int i = 0; i < shapes.length; i++) {
            int x = tileX(i), y = tileY(i);
            if (mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE) {
                PacketDistributor.sendToServer(new ClayMoldItem.ShapePayload(i));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

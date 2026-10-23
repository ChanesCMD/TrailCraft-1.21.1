package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.client.voice.VoiceHeads;
import io.github.ChanesCMD.trailcraft.voice.VoicePayloads;
import io.github.ChanesCMD.trailcraft.voice.VoiceServer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/** Входящий звонок: «Звонок от …» и кнопки «Принять / Отклонить». Не ставит игру на паузу; сам закрывается, когда вызов истёк. */
public class CallScreen extends Screen {
    private static final int W = 170, H = 62;
    private final UUID caller;
    private int ticks;

    public CallScreen(UUID caller, String name) {
        super(Component.translatable("gui.trailcraft.call.incoming", name));
        this.caller = caller;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return height / 4 - H / 2;
    }

    @Override
    protected void init() {
        int x = left(), y = top() + H - 26;
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.call.accept"), b -> answer(true))
                .bounds(x + 8, y, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.call.decline"), b -> answer(false))
                .bounds(x + W - 82, y, 74, 18).build());
    }

    private void answer(boolean accept) {
        PacketDistributor.sendToServer(new VoicePayloads.Answer(accept));
        onClose();
    }

    @Override
    public void tick() {
        if (++ticks > VoiceServer.RING_TICKS) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = left(), y = top();
        UiDraw.panel(g, x, y, W, H);
        VoiceHeads.head(g, x + 9, y + 9, caller);
        g.drawString(font, title, x + 31, y + 13, UiDraw.LABEL, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Без затемнения: игра видна, звонок — небольшое окно сверху
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

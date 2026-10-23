package io.github.ChanesCMD.trailcraft.client.voice;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;
import java.util.UUID;

/**
 * Головы голосового чата без рамок слотов: голова 16×16 (лицо + слой шапки) с обводкой в 1 px,
 * цвет обводки — состояние: говорит (зелёный), молчит (тёмный), голос или микрофон выключен (красный).
 */
public final class VoiceHeads {
    public static final int SIZE = 16, STEP = 20;

    /** Столбик голов сверху вниз от (x, y) с шагом STEP. */
    public static void column(GuiGraphics g, int x, int y, List<UUID> players) {
        for (int i = 0; i < players.size(); i++) {
            head(g, x, y + i * STEP, players.get(i));
        }
    }

    public static void head(GuiGraphics g, int x, int y, UUID player) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
        if (info == null) {
            return;
        }
        int outline = switch (VoiceClient.state(player)) {
            case TALKING -> UiDraw.GREEN;
            case IDLE -> 0xFF1E1E1E;
            case DISABLED -> 0xFFD83A3A;
        };
        g.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, outline);
        PlayerFaceRenderer.draw(g, info.getSkin(), x, y, SIZE);
    }

    private VoiceHeads() {}
}

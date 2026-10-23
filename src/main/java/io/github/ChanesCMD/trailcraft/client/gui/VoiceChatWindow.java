package io.github.ChanesCMD.trailcraft.client.gui;

import io.github.ChanesCMD.trailcraft.client.voice.VoiceClient;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceHeads;
import io.github.ChanesCMD.trailcraft.voice.CommDevices;
import io.github.ChanesCMD.trailcraft.voice.VoiceChannel;
import io.github.ChanesCMD.trailcraft.voice.VoicePayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * Окно голосового чата 200×178 (ТЗ, раздел 7; макет docs/ui/07_voice_chat.png): каналы party/city/country/local
 * и звонки игрокам. Без рации (если это включено на сервере) удалённые каналы и звонки заблокированы.
 */
public class VoiceChatWindow {
    public static final int W = 200, H = 178;
    private static final int ROW_X = 7, ROW_W = 186, CHANNEL_Y = 30, CHANNEL_H = 14, CHANNEL_STEP = 16;
    private static final int CALL_Y = 110, CALL_H = 20, CALL_STEP = 21, CALL_ROWS = 3, CALL_BUTTON_W = 56;
    private static final int GEAR = 14;

    private final Font font;
    public int x, y;
    private int scroll;

    public VoiceChatWindow(Font font) {
        this.font = font;
    }

    /** Окно открывается под кнопкой голосового чата и не вылезает за экран. */
    public void place(int buttonX, int screenWidth, int screenHeight) {
        x = Math.max(4, Math.min(buttonX, screenWidth - W - 4));
        y = Math.max(4, Math.min(CornerButtons.Y + CornerButtons.SIZE + 4, screenHeight - H - 4));
    }

    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + W && mouseY >= y && mouseY < y + H;
    }

    private List<UUID> callTargets() {
        Minecraft mc = Minecraft.getInstance();
        UUID self = mc.player == null ? null : mc.player.getUUID();
        return VoiceClient.connectedPlayers().stream().filter(id -> !id.equals(self)).toList();
    }

    private static boolean remoteAllowed() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && CommDevices.allowsRemoteVoice(mc.player);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        UiDraw.panel(g, x, y, W, H);
        g.drawString(font, Component.translatable("gui.trailcraft.voice"), x + 7, y + 7, UiDraw.LABEL, false);
        drawGear(g, x + W - GEAR - 5, y + 4);
        if (!VoiceClient.available()) {
            g.drawWordWrap(font, Component.translatable("gui.trailcraft.voice.unavailable"), x + 7, y + 30, W - 14, 0xFF606060);
            return;
        }
        boolean remote = remoteAllowed();
        g.drawString(font, Component.translatable("gui.trailcraft.voice.channels"), x + 7, y + 20, 0xFF606060, false);
        VoiceChannel current = VoiceClient.currentChannel();
        VoiceChannel[] channels = VoiceChannel.values();
        for (int i = 0; i < channels.length; i++) {
            VoiceChannel channel = channels[i];
            int ry = y + CHANNEL_Y + i * CHANNEL_STEP;
            boolean locked = channel.remote() && !remote;
            boolean selected = channel == current;
            int fill = selected ? 0xFF55B04F : locked ? 0xFF5A5A5A : 0xFF8B8B8B;
            UiDraw.frame(g, x + ROW_X, ry, ROW_W, CHANNEL_H, selected ? 0xFFFFFFFF : 0xFF373737, 0xFFFFFFFF, fill);
            UiDraw.circle(g, x + ROW_X + 4, ry + 4, 5, selected ? 0xFFB8F2B0 : 0xFF3A3A3A);
            g.drawString(font, channel.id(), x + ROW_X + 13, ry + 3, locked ? 0xFFA0A0A0 : 0xFFFFFFFF, false);
            Component right = locked ? Component.translatable("gui.trailcraft.voice.need_device")
                    : Component.translatable("gui.trailcraft.voice.in_channel", VoiceClient.count(channel));
            g.drawString(font, right, x + ROW_X + ROW_W - 4 - font.width(right), ry + 3, locked ? 0xFFC0C0C0 : 0xFFFFFFFF, false);
            if (locked) {
                drawLock(g, x + ROW_X + ROW_W - 14 - font.width(right), ry + 3);
            }
        }

        g.drawString(font, Component.translatable("gui.trailcraft.voice.calls"), x + 7, y + CALL_Y - 10, 0xFF606060, false);
        List<UUID> targets = callTargets();
        if (targets.isEmpty()) {
            g.drawString(font, Component.translatable("gui.trailcraft.voice.no_players"), x + ROW_X + 2, y + CALL_Y + 4, 0xFF606060, false);
        }
        UUID partner = VoiceClient.callPartner();
        for (int i = 0; i < CALL_ROWS && scroll + i < targets.size(); i++) {
            UUID target = targets.get(scroll + i);
            int ry = y + CALL_Y + i * CALL_STEP;
            UiDraw.frame(g, x + ROW_X, ry, ROW_W, CALL_H, 0xFF373737, 0xFFFFFFFF, 0xFF8B8B8B);
            VoiceHeads.head(g, x + ROW_X + 2, ry + 2, target);
            PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(target);
            g.drawString(font, info == null ? "?" : info.getProfile().getName(), x + ROW_X + 24, ry + 6, 0xFFFFFFFF, false);
            boolean inCall = target.equals(partner);
            boolean active = inCall || remote;
            int bx = x + ROW_X + ROW_W - CALL_BUTTON_W - 3, by = ry + 3;
            boolean hover = active && mouseX >= bx && mouseX < bx + CALL_BUTTON_W && mouseY >= by && mouseY < by + 14;
            UiDraw.frame(g, bx, by, CALL_BUTTON_W, 14, 0xFF000000, hover ? 0xFFFFFFFF : 0xFFA0A0A0, active ? (hover ? 0xFF9A9AC8 : 0xFF6E6E6E) : 0xFF4A4A4A);
            Component label = Component.translatable(inCall ? "gui.trailcraft.voice.hangup" : "gui.trailcraft.voice.call");
            g.drawString(font, label, bx + (CALL_BUTTON_W - font.width(label)) / 2, by + 3, active ? 0xFFFFFFFF : 0xFF8A8A8A, false);
        }
    }

    /** Шестерёнка 14×14. */
    private void drawGear(GuiGraphics g, int gx, int gy) {
        UiDraw.frame(g, gx, gy, GEAR, GEAR, 0xFF000000, 0xFFFFFFFF, 0xFF6E6E6E);
        int c = 0xFFFFFFFF;
        g.fill(gx + 5, gy + 2, gx + 9, gy + 12, c);
        g.fill(gx + 2, gy + 5, gx + 12, gy + 9, c);
        g.fill(gx + 3, gy + 3, gx + 11, gy + 11, c);
        g.fill(gx + 5, gy + 5, gx + 9, gy + 9, 0xFF6E6E6E);
    }

    private void drawLock(GuiGraphics g, int lx, int ly) {
        int c = 0xFFC0C0C0;
        g.fill(lx + 2, ly, lx + 6, ly + 1, c);
        g.fill(lx + 1, ly + 1, lx + 2, ly + 3, c);
        g.fill(lx + 6, ly + 1, lx + 7, ly + 3, c);
        g.fill(lx, ly + 3, lx + 8, ly + 8, c);
    }

    /** @return true, если клик обработан окном */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isOver(mouseX, mouseY)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        int gx = x + W - GEAR - 5, gy = y + 4;
        if (mouseX >= gx && mouseX < gx + GEAR && mouseY >= gy && mouseY < gy + GEAR) {
            openVoiceSettings(mc);
            return true;
        }
        if (!VoiceClient.available()) {
            return true;
        }
        VoiceChannel[] channels = VoiceChannel.values();
        for (int i = 0; i < channels.length; i++) {
            int ry = y + CHANNEL_Y + i * CHANNEL_STEP;
            if (mouseX >= x + ROW_X && mouseX < x + ROW_X + ROW_W && mouseY >= ry && mouseY < ry + CHANNEL_H) {
                if (channels[i] != VoiceClient.currentChannel()) {
                    PacketDistributor.sendToServer(new VoicePayloads.SetChannel(channels[i].id()));
                }
                return true;
            }
        }
        List<UUID> targets = callTargets();
        UUID partner = VoiceClient.callPartner();
        for (int i = 0; i < CALL_ROWS && scroll + i < targets.size(); i++) {
            int bx = x + ROW_X + ROW_W - CALL_BUTTON_W - 3, by = y + CALL_Y + i * CALL_STEP + 3;
            if (mouseX >= bx && mouseX < bx + CALL_BUTTON_W && mouseY >= by && mouseY < by + 14) {
                UUID target = targets.get(scroll + i);
                if (target.equals(partner)) {
                    PacketDistributor.sendToServer(VoicePayloads.Hangup.INSTANCE);
                } else if (remoteAllowed()) {
                    PacketDistributor.sendToServer(new VoicePayloads.Call(target));
                }
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isOver(mouseX, mouseY)) {
            return false;
        }
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), callTargets().size() - CALL_ROWS));
        return true;
    }

    /** Настройки Simple Voice Chat открываются его же клавишей: закрываем инвентарь и «нажимаем» её. */
    private static void openVoiceSettings(Minecraft mc) {
        for (KeyMapping key : mc.options.keyMappings) {
            if (key.getName().equals("key.voice_chat_settings") && !key.isUnbound()) {
                mc.setScreen(null);
                KeyMapping.click(key.getKey());
                return;
            }
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("gui.trailcraft.voice.unavailable"), true);
        }
    }
}

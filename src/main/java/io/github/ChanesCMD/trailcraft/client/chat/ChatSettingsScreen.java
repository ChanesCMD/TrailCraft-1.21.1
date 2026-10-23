package io.github.ChanesCMD.trailcraft.client.chat;

import io.github.ChanesCMD.trailcraft.chat.ChatChannels;
import io.github.ChanesCMD.trailcraft.chat.ChatPayloads;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Настройки чатов (ТЗ 5.2–5.3, макет docs/ui/12_chat_settings.png). Слева «Сервер: чаты» — только операторам:
 * обязательные каналы с замком, «Удалить» у дополнительных, галочка голосовых устройств, «+ Добавить чат».
 * Справа «Настройки: чаты»: показ дополнительных каналов у себя, обязательные — «Всегда видно».
 */
public class ChatSettingsScreen extends Screen {
    private static final int MAX_PANEL_W = 220, ROW_H = 15, ROW_STEP = 17, TOP = 20, GAP = 12, BUTTON_W = 84;

    private final Screen parent;
    private boolean adding;
    @Nullable
    private EditBox nameBox;

    public ChatSettingsScreen(Screen parent) {
        super(Component.translatable("gui.trailcraft.chat.settings"));
        this.parent = parent;
    }

    /** Ширина панели: до MAX_PANEL_W, но две панели с отступами должны влезть в экран. */
    private int panelW() {
        int panels = isOperator() ? 2 : 1;
        return Math.min(MAX_PANEL_W, (width - 8 - GAP * (panels - 1)) / panels);
    }

    private boolean isOperator() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(ChatChannels.EDIT_PERMISSION);
    }

    private List<String> channels() {
        return ChatChannels.all(ChatClient.extras());
    }

    /** Высота подписи галочки (переносится на несколько строк). */
    private int ruleHeight() {
        return Math.max(20, font.split(Component.translatable("gui.trailcraft.chat.voice_rule"), panelW() - 14 - 24).size() * 9 + 4);
    }

    private int hintHeight() {
        return font.split(Component.translatable("gui.trailcraft.chat.hint"), panelW() - 14).size() * 9;
    }

    private int serverHeight() {
        return TOP + channels().size() * ROW_STEP + 2 + ruleHeight() + 4 + (adding ? 40 : 22);
    }

    private int clientHeight() {
        return TOP + channels().size() * ROW_STEP + 4 + hintHeight() + 6;
    }

    private int serverX() {
        return isOperator() ? (width - panelW() * 2 - GAP) / 2 : -1000;
    }

    private int clientX() {
        return isOperator() ? serverX() + panelW() + GAP : (width - panelW()) / 2;
    }

    private int panelY(int panelHeight) {
        return Math.max(4, (height - panelHeight) / 2);
    }

    /** Сервер прислал новый список каналов — перестроить кнопки. */
    public void refresh() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        List<String> channels = channels();
        if (isOperator()) {
            int x = serverX(), y = panelY(serverHeight());
            for (int i = 0; i < channels.size(); i++) {
                String channel = channels.get(i);
                if (!ChatChannels.isRequired(channel)) {
                    addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.chat.remove"),
                                    b -> edit(ChatPayloads.Edit.Action.REMOVE, channel))
                            .bounds(x + panelW() - 7 - 52, y + TOP + i * ROW_STEP + 1, 50, ROW_H - 2).build());
                }
            }
            int checkY = y + TOP + channels.size() * ROW_STEP + 2;
            addRenderableWidget(Checkbox.builder(Component.translatable("gui.trailcraft.chat.voice_rule"), font)
                    .pos(x + 7, checkY).maxWidth(panelW() - 22).selected(ChatClient.voiceRequiresDevice())
                    .onValueChange((box, value) -> edit(ChatPayloads.Edit.Action.VOICE_REQUIRES_DEVICE, String.valueOf(value))).build());
            int addY = checkY + ruleHeight() + 4;
            if (adding) {
                nameBox = new EditBox(font, x + 7, addY, panelW() - 14 - 34, 16, Component.translatable("gui.trailcraft.chat.name"));
                nameBox.setHint(Component.translatable("gui.trailcraft.chat.name"));
                nameBox.setMaxLength(16);
                nameBox.setFilter(s -> s.matches("[a-z0-9_-]*"));
                addRenderableWidget(nameBox);
                setInitialFocus(nameBox);
                addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.chat.ok"), b -> addChannel())
                        .bounds(x + panelW() - 7 - 30, addY, 30, 16).build());
                addY += 20;
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.trailcraft.chat.add"), b -> {
                        adding = !adding;
                        rebuildWidgets();
                    })
                    .bounds(x + 7, addY, panelW() - 14, 16).build());
        }
        int x = clientX(), y = panelY(clientHeight());
        for (int i = 0; i < channels.size(); i++) {
            String channel = channels.get(i);
            boolean required = ChatChannels.isRequired(channel);
            Component label = Component.translatable(required ? "gui.trailcraft.chat.always"
                    : ClientConfig.isHidden(channel) ? "gui.trailcraft.chat.hidden" : "gui.trailcraft.chat.shown");
            Button button = addRenderableWidget(Button.builder(label, b -> {
                        ClientConfig.setHidden(channel, !ClientConfig.isHidden(channel));
                        rebuildWidgets();
                    })
                    .bounds(x + panelW() - 7 - BUTTON_W, y + TOP + i * ROW_STEP + 1, BUTTON_W, ROW_H - 2).build());
            button.active = !required;
        }
    }

    private void addChannel() {
        if (nameBox != null && !nameBox.getValue().isBlank()) {
            edit(ChatPayloads.Edit.Action.ADD, nameBox.getValue());
        }
        adding = false;
        rebuildWidgets();
    }

    private void edit(ChatPayloads.Edit.Action action, String value) {
        PacketDistributor.sendToServer(new ChatPayloads.Edit(action, value));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (adding && nameBox != null && nameBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
            addChannel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        List<String> channels = channels();
        if (isOperator()) {
            int x = serverX(), y = panelY(serverHeight());
            UiDraw.panel(g, x, y, panelW(), serverHeight());
            g.drawString(font, Component.translatable("gui.trailcraft.chat.server"), x + 7, y + 7, UiDraw.LABEL, false);
            for (int i = 0; i < channels.size(); i++) {
                String channel = channels.get(i);
                int ry = y + TOP + i * ROW_STEP;
                row(g, x, ry, channel);
                if (ChatChannels.isRequired(channel)) {
                    Component required = Component.translatable("gui.trailcraft.chat.required");
                    int tx = x + panelW() - 10 - font.width(required);
                    g.drawString(font, required, tx, ry + 4, 0xFF404040, false);
                    lock(g, tx - 9, ry + 3);
                }
            }
        }
        int x = clientX(), y = panelY(clientHeight());
        UiDraw.panel(g, x, y, panelW(), clientHeight());
        g.drawString(font, Component.translatable("gui.trailcraft.chat.client"), x + 7, y + 7, UiDraw.LABEL, false);
        for (int i = 0; i < channels.size(); i++) {
            row(g, x, y + TOP + i * ROW_STEP, channels.get(i));
        }
        g.drawWordWrap(font, Component.translatable("gui.trailcraft.chat.hint"), x + 7, y + TOP + channels.size() * ROW_STEP + 4, panelW() - 14, 0xFF505050);
    }

    private void row(GuiGraphics g, int x, int y, String channel) {
        UiDraw.frame(g, x + 7, y, panelW() - 14, ROW_H, 0xFF373737, 0xFFFFFFFF, 0xFF8B8B8B);
        g.drawString(font, channel, x + 11, y + 4, 0xFFFFFFFF, false);
    }

    private void lock(GuiGraphics g, int lx, int ly) {
        int c = 0xFF404040;
        g.fill(lx + 2, ly, lx + 5, ly + 1, c);
        g.fill(lx + 1, ly + 1, lx + 2, ly + 3, c);
        g.fill(lx + 5, ly + 1, lx + 6, ly + 3, c);
        g.fill(lx, ly + 3, lx + 7, ly + 8, c);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package io.github.ChanesCMD.trailcraft.client.chat;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.chat.ChatChannels;
import io.github.ChanesCMD.trailcraft.chat.ChatPayloads;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.voice.CommDevices;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Каналы чата на клиенте (ТЗ 5.1, макет docs/ui/11_chat.png): вкладки над окном чата, сообщение уходит в канал
 * активной вкладки; справа от вкладок — шестерёнка настроек чатов. Сообщения скрытых каналов не показываются.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class ChatClient {
    private static final int TAB_H = 13, TAB_PAD = 5, TAB_GAP = 2;

    private static List<String> extras = List.of("trading");
    private static boolean voiceRequiresDevice = true;
    private static String activeChannel = ChatChannels.GLOBAL;

    /** Вкладки: все каналы сервера, кроме скрытых у себя дополнительных. */
    public static List<String> tabs() {
        return ChatChannels.all(extras).stream().filter(c -> ChatChannels.isRequired(c) || !ClientConfig.isHidden(c)).toList();
    }

    public static List<String> extras() {
        return extras;
    }

    public static boolean voiceRequiresDevice() {
        return voiceRequiresDevice;
    }

    // === Пакеты от сервера ===

    public static void channels(ChatPayloads.Channels state) {
        extras = state.extras();
        voiceRequiresDevice = state.voiceRequiresDevice();
        CommDevices.setClientRule(voiceRequiresDevice);
        if (!tabs().contains(activeChannel)) {
            activeChannel = ChatChannels.GLOBAL;
        }
        if (Minecraft.getInstance().screen instanceof ChatSettingsScreen screen) {
            screen.refresh();
        }
    }

    public static void receive(ChatPayloads.Message message) {
        if (!ClientConfig.isHidden(message.channel())) {
            Minecraft.getInstance().gui.getChat().addMessage(message.text());
        }
    }

    // === События ===

    /** Сообщение не в global — отправляем своим пакетом в канал активной вкладки. Команды сюда не попадают. */
    @SubscribeEvent
    public static void onSend(ClientChatEvent event) {
        if (!activeChannel.equals(ChatChannels.GLOBAL)) {
            PacketDistributor.sendToServer(new ChatPayloads.Send(activeChannel, event.getMessage()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        activeChannel = ChatChannels.GLOBAL;
    }

    /** Вкладки и шестерёнка появляются вместе с окном чата, над его верхним краем. */
    @SubscribeEvent
    public static void onChatScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int y = tabsY(screen.height);
        int x = 2;
        for (String channel : tabs()) {
            Tab tab = new Tab(x, y, mc.font.width(channel) + TAB_PAD * 2, channel);
            event.addListener(tab);
            x += tab.getWidth() + TAB_GAP;
        }
        Gear gear = new Gear(x + 2, y, screen);
        gear.setTooltip(Tooltip.create(Component.translatable("gui.trailcraft.chat.settings")));
        event.addListener(gear);
    }

    /** Верх вкладок: над областью открытого чата (она выше полосы ввода на 40 px). */
    private static int tabsY(int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int chatHeight = (int) Math.ceil(mc.gui.getChat().getHeight() * mc.gui.getChat().getScale());
        return Math.max(2, screenHeight - 40 - chatHeight - TAB_H - 2);
    }

    /**
     * Пока чат открыт, под всей его областью — тёмная подложка (как на макете), чтобы вкладки сверху и сообщения
     * были одним окном. Слой рисуется под ванильным чатом, поэтому сообщения поверх неё.
     */
    private static void renderChatBackground(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen && !mc.options.hideGui) {
            int width = (int) Math.ceil(mc.gui.getChat().getWidth() * mc.gui.getChat().getScale()) + 8;
            g.fill(0, tabsY(g.guiHeight()) + TAB_H, width, g.guiHeight() - 38, 0x60000000);
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerBelow(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "chat_background"),
                    ChatClient::renderChatBackground);
        }
    }

    /** Вкладка канала: тёмная, активная — светлее. */
    private static class Tab extends AbstractWidget {
        private final String channel;

        Tab(int x, int y, int width, String channel) {
            super(x, y, width, TAB_H, Component.literal(channel));
            this.channel = channel;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = channel.equals(activeChannel);
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                    selected ? 0xFF8B8B8B : isHovered() ? 0xFF4A4A4A : 0xFF2E2E2E);
            g.drawString(Minecraft.getInstance().font, channel, getX() + TAB_PAD, getY() + 3, selected ? 0xFFFFFFFF : 0xFFA0A0A0, selected);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            activeChannel = channel;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Шестерёнка 13×13: открывает настройки чатов. */
    private static class Gear extends AbstractWidget {
        private final ChatScreen parent;

        Gear(int x, int y, ChatScreen parent) {
            super(x, y, TAB_H, TAB_H, Component.translatable("gui.trailcraft.chat.settings"));
            this.parent = parent;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), c = isHovered() ? 0xFFFFFFFF : 0xFFC0C0C0;
            g.fill(x, y, x + width, y + height, 0xFF000000);
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2E2E2E);
            g.fill(x + 5, y + 2, x + 8, y + 11, c);
            g.fill(x + 2, y + 5, x + 11, y + 8, c);
            g.fill(x + 3, y + 3, x + 10, y + 10, c);
            g.fill(x + 5, y + 5, x + 8, y + 8, 0xFF2E2E2E);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            Minecraft.getInstance().setScreen(new ChatSettingsScreen(parent));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private ChatClient() {}
}

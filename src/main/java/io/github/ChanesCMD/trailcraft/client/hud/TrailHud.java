package io.github.ChanesCMD.trailcraft.client.hud;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.client.ClientGear;
import io.github.ChanesCMD.trailcraft.client.Coords;
import io.github.ChanesCMD.trailcraft.client.map.Minimap;
import io.github.ChanesCMD.trailcraft.client.gui.TrailInventoryScreen;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceClient;
import io.github.ChanesCMD.trailcraft.client.voice.VoiceHeads;
import io.github.ChanesCMD.trailcraft.health.BodyHealth;
import io.github.ChanesCMD.trailcraft.health.BodyHealth.Part;
import io.github.ChanesCMD.trailcraft.stamina.Stamina;
import io.github.ChanesCMD.trailcraft.stamina.StaminaEvents;
import io.github.ChanesCMD.trailcraft.water.WaterEvents;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD по ТЗ (раздел 4): вместо сердечек — еда (морковки), над ней вместо брони — выносливость, вместо еды — вода,
 * над ней — кислород; слева от хотбара — модульное HP. Значки 9×9 с шагом 8, как в ванилле.
 * Уровень брони не выводится: его показывает подсказка каждого предмета брони.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TrailHud {
    private static final int ROW = 10, STEP = 8;
    /** Цвета частей тела по состоянию (ТЗ, 4.1) и контур. */
    private static final int OUTLINE = 0xFF1A1A1A, DESTROYED_OUTLINE = 0xFF6A6A6A;

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, TrailHud::renderFoodAndBody);
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, TrailHud::renderStamina);
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, TrailHud::renderWater);
        event.replaceLayer(VanillaGuiLayers.AIR_LEVEL, TrailHud::renderAir);
        event.registerBelow(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "voice_heads"), TrailHud::renderVoiceHeads);
        event.registerBelow(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "map_and_date"), TrailHud::renderMapAndDate);
    }

    /**
     * replaceLayer заменяет слой целиком вместе с условиями ванильных групп (NeoForge их разворачивает),
     * поэтому проверяем их сами: интерфейс не скрыт и игрок может получать урон (не креатив и не наблюдатель).
     */
    private static boolean visible(Minecraft mc) {
        return !mc.options.hideGui && mc.gameMode != null && mc.gameMode.canHurtPlayer();
    }

    /**
     * Головы голосового чата у левого края (x=6, y=20+20·i), когда в нём хотя бы двое; если миникарта или дата стоят
     * в левом верхнем углу — правее них. Пока открыт инвентарь, головы нарисованы там (слева от панели), а здесь их нет.
     */
    private static void renderVoiceHeads(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui && !(mc.screen instanceof TrailInventoryScreen) && VoiceClient.showHeads()) {
            int x = 6;
            if (ClientConfig.minimapCorner() == ClientConfig.Corner.TOP_LEFT) {
                boolean map = ClientGear.hasAtlas() && ClientConfig.minimapEnabled();
                x += Math.max(map ? Minimap.size() : 0, ClientGear.hasCalendar() || ClientGear.hasAtlas() ? 110 : 0) + 6;
            }
            VoiceHeads.column(g, x, 20, VoiceClient.heads());
        }
    }

    /**
     * Миникарта (если в рюкзаке атлас и она включена) в выбранном углу экрана (отступ 6); рядом с ней строки:
     * биом и координаты (с атласом, если включены), дата «Год N · Сезон, день» и время (с календарём). В верхних
     * углах строки под картой, в нижних над ней; в правых углах прижаты к правому краю.
     */
    private static void renderMapAndDate(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        ClientConfig.Corner corner = ClientConfig.minimapCorner();
        boolean atlas = ClientGear.hasAtlas(), showMap = atlas && ClientConfig.minimapEnabled();
        int size = Minimap.size();
        List<Component> lines = new ArrayList<>();
        if (atlas && ClientConfig.minimapBiome()) {
            lines.add(player.level().getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> (Component) Component.translatable(Util.makeDescriptionId("biome", key.location())))
                    .orElse(Component.empty()));
        }
        // Координаты видны и без атласа; точность — по приборам на поясе (Coords)
        if (ClientConfig.minimapCoords()) {
            lines.add(Component.literal(Coords.of(player.getBlockX(), player.getBlockY(), player.getBlockZ())));
        }
        ClientConfig.DateMode dateMode = ClientConfig.dateMode();
        if (ClientGear.hasCalendar() && dateMode != ClientConfig.DateMode.OFF) {
            Calendar date = Calendar.of(player.level().getDayTime(), ServerConfig.daysPerSeason());
            if (dateMode.showDate()) {
                lines.add(Component.translatable("gui.trailcraft.hud.date", date.year(),
                        Component.translatable(date.season().translationKey()), date.dayOfSeason()));
            }
            if (dateMode.showTime()) {
                lines.add(Component.literal(date.time()));
            }
        }
        int textH = lines.size() * 10, gap = showMap && textH > 0 ? 3 : 0;
        int blockH = (showMap ? size : 0) + gap + textH;
        int top = corner.bottom() ? g.guiHeight() - 6 - blockH : 6;
        int mapY = corner.bottom() ? top + textH + gap : top;
        int textY = corner.bottom() ? top : top + (showMap ? size : 0) + gap;
        int right = g.guiWidth() - 6;
        if (showMap) {
            Minimap.render(g, corner.left() ? 6 : right - size, mapY, player, delta.getGameTimeDeltaPartialTick(true));
        }
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            g.drawString(mc.font, line, corner.left() ? 6 : right - mc.font.width(line), textY + i * 10, 0xFFFFFFFF, true);
        }
    }

    /** 10 морковок слева над хотбаром; фигурка HP слева от хотбара, если здоровье не полное. */
    private static void renderFoodAndBody(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !visible(mc)) {
            return;
        }
        int[] colors = player.hasEffect(MobEffects.HUNGER) ? HudIcons.HUNGER_COLORS : HudIcons.FOOD_COLORS;
        // Ряд еды с насыщением, истощением и тем, что даст еда в руке (FoodOverlay, как AppleSkin)
        FoodOverlay.render(g, player, g.guiWidth() / 2 - 91, g.guiHeight() - mc.gui.leftHeight, STEP, colors);
        mc.gui.leftHeight += ROW;
        if (!BodyHealth.isFull(player)) {
            // От верха ряда еды до низа хотбара — 39 px, фигурка 35
            renderBody(g, g.guiWidth() / 2 - 91 - 6 - 18, g.guiHeight() - 37, player);
        }
    }

    /** Выносливость над едой, только пока она не полная; потраченные деления видны. */
    private static void renderStamina(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !visible(mc)) {
            return;
        }
        float stamina = StaminaEvents.get(player);
        if (stamina >= Stamina.MAX) {
            return;
        }
        drawLeftRow(g, mc, HudIcons.BOLT, HudIcons.STAMINA, Mth.ceil(stamina / Stamina.MAX * 20));
    }

    /** 10 капель справа над хотбаром, заполняются справа налево. Верхом на животном — его здоровье, как в ванилле. */
    private static void renderWater(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !visible(mc) || player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
            return;
        }
        drawRightRow(g, mc, HudIcons.DROP, HudIcons.WATER, WaterEvents.get(player));
    }

    /** Кислород над водой, только пока он тратится; пустые пузыри не рисуются, как в ванилле. */
    private static void renderAir(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !visible(mc)) {
            return;
        }
        int air = Math.max(0, player.getAirSupply()), max = player.getMaxAirSupply();
        if (air >= max) {
            return;
        }
        int right = g.guiWidth() / 2 + 91, y = g.guiHeight() - mc.gui.rightHeight;
        int bubbles = Mth.ceil(air * 10.0 / max);
        for (int i = 0; i < bubbles; i++) {
            HudIcons.draw(g, right - i * STEP - 9, y, HudIcons.BUBBLE, HudIcons.AIR, 2);
        }
        mc.gui.rightHeight += ROW;
    }

    /** Ряд из 10 значков слева над хотбаром, заполняется слева направо; halves — 0..20. */
    private static void drawLeftRow(GuiGraphics g, Minecraft mc, String[] icon, int[] colors, int halves) {
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - mc.gui.leftHeight;
        for (int i = 0; i < 10; i++) {
            HudIcons.draw(g, x + i * STEP, y, icon, colors, Mth.clamp(halves - 2 * i, 0, 2));
        }
        mc.gui.leftHeight += ROW;
    }

    /** Ряд из 10 значков справа над хотбаром, заполняется справа налево; halves — 0..20. */
    private static void drawRightRow(GuiGraphics g, Minecraft mc, String[] icon, int[] colors, int halves) {
        int right = g.guiWidth() / 2 + 91, y = g.guiHeight() - mc.gui.rightHeight;
        for (int i = 0; i < 10; i++) {
            HudIcons.draw(g, right - i * STEP - 9, y, icon, colors, Mth.clamp(halves - 2 * i, 0, 2), true);
        }
        mc.gui.rightHeight += ROW;
    }

    /** Фигурка 18×35: голова 8×8, туловище 8×12, руки 4×12, ноги 4×13, зазоры 1 px. */
    private static void renderBody(GuiGraphics g, int x, int y, Player player) {
        part(g, x + 5, y, 8, 8, BodyHealth.state(player, Part.HEAD));
        part(g, x + 5, y + 9, 8, 12, BodyHealth.state(player, Part.TORSO));
        part(g, x, y + 9, 4, 12, BodyHealth.state(player, Part.RIGHT_ARM));
        part(g, x + 14, y + 9, 4, 12, BodyHealth.state(player, Part.LEFT_ARM));
        part(g, x + 5, y + 22, 4, 13, BodyHealth.state(player, Part.RIGHT_LEG));
        part(g, x + 9, y + 22, 4, 13, BodyHealth.state(player, Part.LEFT_LEG));
    }

    private static void part(GuiGraphics g, int x, int y, int w, int h, BodyHealth.State state) {
        int color = switch (state) {
            case INTACT -> 0xFF9A9A9A;
            case DAMAGED -> 0xFFE8C33A;
            case CRITICAL -> 0xFFD83A3A;
            case DESTROYED -> 0xFF111111;
            case PROSTHESIS -> 0xFF3F76E4;
        };
        g.fill(x, y, x + w, y + h, state == BodyHealth.State.DESTROYED ? DESTROYED_OUTLINE : OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }
}

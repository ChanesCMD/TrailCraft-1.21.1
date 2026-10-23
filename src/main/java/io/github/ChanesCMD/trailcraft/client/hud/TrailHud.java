package io.github.ChanesCMD.trailcraft.client.hud;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.health.BodyHealth;
import io.github.ChanesCMD.trailcraft.health.BodyHealth.Part;
import io.github.ChanesCMD.trailcraft.stamina.Stamina;
import io.github.ChanesCMD.trailcraft.stamina.StaminaEvents;
import io.github.ChanesCMD.trailcraft.water.Water;
import io.github.ChanesCMD.trailcraft.water.WaterEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD по ТЗ (раздел 4): вместо сердечек — вода, вместо брони — кислород, еда на своём месте, вместо воздуха —
 * выносливость; слева от хотбара — модульное HP. Значки 9×9 с шагом 8, как в ванилле.
 * Уровень брони не выводится: его показывает подсказка каждого предмета брони.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TrailHud {
    private static final int ROW = 10, STEP = 8;
    /** Цвета частей тела по состоянию (ТЗ, 4.1) и контур. */
    private static final int OUTLINE = 0xFF1A1A1A, DESTROYED_OUTLINE = 0xFF6A6A6A;

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, TrailHud::renderWaterAndBody);
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, TrailHud::renderAir);
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, TrailHud::renderFood);
        event.replaceLayer(VanillaGuiLayers.AIR_LEVEL, TrailHud::renderStamina);
    }

    /** 10 капель слева над хотбаром; фигурка HP слева от хотбара, если здоровье не полное. */
    private static void renderWaterAndBody(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - mc.gui.leftHeight;
        int water = WaterEvents.get(player);
        for (int i = 0; i < Water.MAX / 2; i++) {
            HudIcons.draw(g, x + i * STEP, y, HudIcons.DROP, HudIcons.WATER, Mth.clamp(water - 2 * i, 0, 2));
        }
        mc.gui.leftHeight += ROW;
        if (!BodyHealth.isFull(player)) {
            // От верха ряда воды до низа хотбара — 39 px, фигурка 35
            renderBody(g, x - 6 - 18, g.guiHeight() - 37, player);
        }
    }

    /** Кислород над водой, только пока он тратится; пустые пузыри не рисуются, как в ванилле. */
    private static void renderAir(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        int air = Math.max(0, player.getAirSupply()), max = player.getMaxAirSupply();
        if (air >= max) {
            return;
        }
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - mc.gui.leftHeight;
        int bubbles = Mth.ceil(air * 10.0 / max);
        for (int i = 0; i < bubbles; i++) {
            HudIcons.draw(g, x + i * STEP, y, HudIcons.BUBBLE, HudIcons.AIR, 2);
        }
        mc.gui.leftHeight += ROW;
    }

    /** 10 значков еды справа над хотбаром, заполняются справа налево. Верхом на животном — его здоровье, как в ванилле. */
    private static void renderFood(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
            return;
        }
        int food = player.getFoodData().getFoodLevel();
        int[] colors = player.hasEffect(MobEffects.HUNGER) ? HudIcons.HUNGER_COLORS : HudIcons.FOOD_COLORS;
        drawRightRow(g, mc, HudIcons.FOOD, colors, food);
    }

    /** Выносливость над едой, только пока она не полная; потраченные деления видны. */
    private static void renderStamina(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        float stamina = StaminaEvents.get(player);
        if (stamina >= Stamina.MAX) {
            return;
        }
        drawRightRow(g, mc, HudIcons.BOLT, HudIcons.STAMINA, Mth.ceil(stamina / Stamina.MAX * 20));
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

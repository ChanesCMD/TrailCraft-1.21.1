package io.github.ChanesCMD.trailcraft.client.hud;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Свой AppleSkin для ряда морковок: насыщение — золотой контур поверх морковок, истощение — тёмная полоска позади
 * ряда, которая убывает до следующей потери насыщения или еды; а если в руке еда — мигают морковки и контур, которые
 * она добавит. Такая же полоска позади ряда воды — сколько осталось до потери следующей единицы воды.
 * В подсказке еды — сколько она даёт. Насыщение клиент получает от ванилы, истощение и жажду — от сервера (FoodSync).
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class FoodOverlay {
    private static final int SATURATION = 0xFFFFD84A, BAR = 0x68000000;
    /** Ванильный порог истощения: на 4 снимается единица насыщения или еды. */
    private static final float EXHAUSTION_MAX = 4;
    private static float exhaustion, thirst;

    /** @param thirst накопленная жажда 0..1 (на 1 тратится единица воды, Water.Tracker) */
    public static void set(float exhaustion, float thirst) {
        FoodOverlay.exhaustion = exhaustion;
        FoodOverlay.thirst = thirst;
    }

    /** Полоска позади ряда воды (ряд заполняется справа налево, поэтому полоска прижата к правому краю right). */
    static void renderThirst(GuiGraphics g, int right, int y, int step) {
        int width = 9 * step + 9;
        int left = Math.round((1 - Mth.clamp(thirst, 0, 1)) * width);
        bar(g, right - left, y, right);
    }

    /** Ряд из 10 морковок слева направо с левого края x, верх — y. */
    static void render(GuiGraphics g, Player player, int x, int y, int step, int[] colors) {
        FoodData food = player.getFoodData();
        int level = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        int width = 9 * step + 9;
        int left = Math.round((1 - Mth.clamp(exhaustion / EXHAUSTION_MAX, 0, 1)) * width);
        bar(g, x, y, x + left);
        for (int i = 0; i < 10; i++) {
            HudIcons.draw(g, x + i * step, y, HudIcons.FOOD, colors, Mth.clamp(level - 2 * i, 0, 2));
        }
        drawSaturation(g, x, y, step, saturation, SATURATION);

        FoodProperties props = heldFood(player);
        if (props == null || !player.canEat(props.canAlwaysEat())) {
            return;
        }
        // Мигание: прозрачность плавно ходит туда-обратно раз в секунду
        int alpha = (int) (110 + 90 * Mth.sin((System.currentTimeMillis() % 1000) / 1000f * Mth.TWO_PI));
        int after = Math.min(20, level + props.nutrition());
        int[] ghost = HudIcons.withAlpha(colors, alpha);
        for (int i = 0; i < 10; i++) {
            int before = Mth.clamp(level - 2 * i, 0, 2), next = Mth.clamp(after - 2 * i, 0, 2);
            if (next > before) {
                HudIcons.draw(g, x + i * step, y, HudIcons.FOOD, ghost, next);
            }
        }
        float nextSaturation = Math.min(after, saturation + props.saturation());
        if (nextSaturation > saturation) {
            drawSaturation(g, x, y, step, nextSaturation, alpha << 24 | SATURATION & 0xFFFFFF);
        }
    }

    /** Полоска высотой 9 со скруглёнными углами (без угловых пикселей). */
    private static void bar(GuiGraphics g, int x1, int y, int x2) {
        if (x2 - x1 < 3) {
            return;
        }
        g.fill(x1 + 1, y, x2 - 1, y + 1, BAR);
        g.fill(x1, y + 1, x2, y + 8, BAR);
        g.fill(x1 + 1, y + 8, x2 - 1, y + 9, BAR);
    }

    private static void drawSaturation(GuiGraphics g, int x, int y, int step, float saturation, int color) {
        int halves = Mth.ceil(saturation);
        for (int i = 0; i < 10; i++) {
            int full = Mth.clamp(halves - 2 * i, 0, 2);
            if (full > 0) {
                HudIcons.drawOutline(g, x + i * step, y, HudIcons.FOOD, color, full);
            }
        }
    }

    /** Еда в основной руке, а если там не еда — в левой. */
    @Nullable
    private static FoodProperties heldFood(Player player) {
        FoodProperties main = player.getMainHandItem().getFoodProperties(player);
        return main != null ? main : player.getOffhandItem().getFoodProperties(player);
    }

    /** Подсказка еды: сколько еды (в морковках) и насыщения она даёт. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        FoodProperties props = stack.getFoodProperties(event.getEntity());
        if (props == null) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.trailcraft.food",
                String.format(Locale.ROOT, "%.1f", props.nutrition() / 2f),
                String.format(Locale.ROOT, "%.1f", props.saturation() / 2f)).withStyle(ChatFormatting.GOLD));
    }

    private FoodOverlay() {}
}

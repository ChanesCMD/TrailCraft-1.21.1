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
 * Свой AppleSkin для ряда морковок: насыщение — золотой контур поверх морковок, истощение — светлая полоска позади
 * ряда (до следующей потери насыщения или еды), а если в руке еда — мигают морковки и контур, которые она добавит.
 * В подсказке еды — сколько она даёт. Насыщение клиент получает от ванилы, истощение — от сервера (FoodSync).
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class FoodOverlay {
    private static final int SATURATION = 0xFFFFD84A;
    /** Ванильный порог истощения: на 4 снимается единица насыщения или еды. */
    private static final float EXHAUSTION_MAX = 4;
    private static float exhaustion;

    public static void setExhaustion(float value) {
        exhaustion = value;
    }

    /** Ряд из 10 морковок слева направо с левого края x, верх — y. */
    static void render(GuiGraphics g, Player player, int x, int y, int step, int[] colors) {
        FoodData food = player.getFoodData();
        int level = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        int width = 9 * step + 9;
        int filled = Math.round(Mth.clamp(exhaustion / EXHAUSTION_MAX, 0, 1) * width);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + 9, 0x40FFFFFF);
        }
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

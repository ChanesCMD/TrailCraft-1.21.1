package io.github.ChanesCMD.trailcraft.climate;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import io.github.ChanesCMD.trailcraft.item.WarmClothingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Холод и игрок: ощущаемая температура = воздух (Climate.air) + огонь рядом (Climate.heat) + тёплая одежда в слотах
 * «Тепло». Ниже {@link #COLD_LIMIT} игрок мёрзнет — как в рыхлом снегу: иней по краям экрана, замедление, а замёрзнув
 * совсем, получает урон (ванильный, 1 раз в 2 с); чем холоднее, тем быстрее замерзает. Ещё — рост культур только
 * в своём диапазоне температуры (CropClimate) и этот диапазон в подсказке.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class ClimateEvents {
    /** Ниже этой ощущаемой температуры (°C) игрок мёрзнет. */
    public static final float COLD_LIMIT = 0f;
    /** Тепло вещи из тэга #trailcraft:warm_clothing, если это не наша одежда (WarmClothingItem). */
    public static final int OTHER_CLOTHING_WARMTH = 3;
    /** На сколько градусов недостатка тепла замерзание идёт на 1 тик быстрее. */
    public static final float DEGREES_PER_RATE = 8f;

    /** Недостаток тепла у игрока (°C), пересчитывается раз в секунду. */
    private static final Map<Player, Float> DEFICIT = new WeakHashMap<>();

    private ClimateEvents() {}

    /** Ощущаемая игроком температура: воздух, огонь рядом, одежда. */
    public static float felt(Player player) {
        Level level = player.level();
        return Climate.air(level, player.blockPosition()) + Climate.heat(level, player.blockPosition()) + insulation(player);
    }

    /** Сколько градусов тепла даёт одежда в слотах «Тепло». */
    public static int insulation(Player player) {
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        int sum = 0;
        for (int i = PlayerSlots.WARMTH; i < PlayerSlots.ACCESSORIES; i++) {
            ItemStack stack = slots.getStackInSlot(i);
            if (stack.getItem() instanceof WarmClothingItem clothing) {
                sum += clothing.warmth();
            } else if (stack.is(PlayerSlots.WARM_CLOTHING)) {
                sum += OTHER_CLOTHING_WARMTH;
            }
        }
        return sum;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            DEFICIT.put(player, COLD_LIMIT - felt(player));
        }
        float deficit = DEFICIT.getOrDefault(player, 0f);
        if (deficit > 0) {
            // ванильный счётчик инея сам убывает на 2 за тик; держим его чуть выше порога «замёрз», чтобы шёл урон
            int rate = 1 + (int) (deficit / DEGREES_PER_RATE);
            int max = player.getTicksRequiredToFreeze() + 2;
            player.setTicksFrozen(Math.min(max, player.getTicksFrozen() + 2 + rate));
        }
    }

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof Level level && !CropClimate.canGrow(level, event.getPos(), event.getState().getBlock())) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        CropClimate.Range range = CropClimate.of(event.getItemStack().getItem());
        if (range != null) {
            event.getToolTip().add(Component.translatable("tooltip.trailcraft.crop_temperature", range.min(), range.max())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.item.SpearItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Свой таймер анимации укола: ванильный взмах длится 6 тиков, тычок копьём — DURATION.
 * Начало укола замечаем по ванильному взмаху (он приходит и от других игроков).
 */
public final class SpearJabAnim {
    public static final int DURATION = 10;
    /** Игровой тик, в который начался укол. */
    private static final Map<LivingEntity, Long> STARTS = new WeakHashMap<>();

    /** Доля пройденного укола 0..1 или -1, если копьём сейчас не колют. */
    public static float progress(LivingEntity entity, float partialTick) {
        if (!(entity.getMainHandItem().getItem() instanceof SpearItem)) {
            return -1;
        }
        long now = entity.level().getGameTime();
        Long start = STARTS.get(entity);
        if (entity.swinging && entity.swingTime <= 1 && (start == null || now - start > 1)) {
            start = now - entity.swingTime;
            STARTS.put(entity, start);
        }
        if (start == null) {
            return -1;
        }
        float time = (now - start + partialTick) / DURATION;
        return time >= 0 && time < 1 ? time : -1;
    }

    /** Насколько выдвинуто копьё (0..1) на доле progress укола: быстро вперёд, медленнее назад. */
    public static float thrust(float progress) {
        return progress < 0.3f ? Mth.sin(progress / 0.3f * Mth.HALF_PI) : Mth.cos((progress - 0.3f) / 0.7f * Mth.HALF_PI);
    }

    private SpearJabAnim() {}
}

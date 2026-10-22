package io.github.ChanesCMD.trailcraft.calendar;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Клиент между пакетами времени с сервера (раз в секунду) сам двигает солнце на 1 тик за тик.
 * Замедляем его так же, как на сервере, чтобы солнце не дёргалось. Настройка приходит с сервера.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientCalendarEvents {
    private static final DayTimeStepper CLIENT_STEPPER = new DayTimeStepper();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ClientLevel level) {
            long time = CLIENT_STEPPER.step(level.getDayTime(), ServerConfig.dayLengthMinutes());
            if (time >= 0) {
                level.setDayTime(time);
            }
        }
    }
}

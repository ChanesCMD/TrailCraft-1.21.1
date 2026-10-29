package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Сезон на сервере и частые дожди осенью. */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class SeasonEvents {
    /** Отсчёт до дождя в прошлый тик (-1 — ещё не видели): по его скачку вверх видно, что игра выбрала новый перерыв. */
    private static int lastRainTime = -1;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        Seasons.update(level.getDayTime());
        ServerLevelData data = level.getServer().getWorldData().overworldData();
        int rainTime = data.getRainTime();
        if (Seasons.current() == Calendar.Season.AUTUMN && lastRainTime >= 0 && !data.isRaining()
                && data.getClearWeatherTime() == 0 && rainTime > lastRainTime + 1) {
            rainTime = Math.max(1, (int) (rainTime * Seasons.AUTUMN_RAIN_DELAY));
            data.setRainTime(rainTime);
        }
        lastRainTime = rainTime;
    }
}

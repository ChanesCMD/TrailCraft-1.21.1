package io.github.ChanesCMD.trailcraft.calendar;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = TrailCraft.MODID)
public class CalendarEvents {
    private static final DayTimeStepper SERVER_STEPPER = new DayTimeStepper();

    /** Время суток общее для всех измерений и хранится в верхнем мире, поэтому замедляем только его. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            long time = SERVER_STEPPER.step(level.getDayTime(), ServerConfig.dayLengthMinutes());
            if (time >= 0) {
                level.setDayTime(time);
            }
        }
    }

    /** /calendar — год, сезон, день и время. Доступна всем игрокам. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("calendar").executes(ctx -> {
            var source = ctx.getSource();
            Calendar cal = Calendar.of(source.getServer().overworld().getDayTime(), ServerConfig.daysPerSeason());
            source.sendSuccess(() -> Component.translatable("commands.trailcraft.calendar",
                    cal.year(), Component.translatable(cal.season().translationKey()),
                    cal.dayOfSeason(), cal.daysPerSeason(), cal.time()), false);
            source.sendSuccess(() -> Component.translatable("commands.trailcraft.calendar.settings",
                    ServerConfig.daysPerSeason(), ServerConfig.dayLengthMinutes()), false);
            // температура там, откуда вызвана команда (climate/Climate): сейчас и средняя за сутки
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(source.getPosition());
            source.sendSuccess(() -> Component.translatable("commands.trailcraft.calendar.temperature",
                    Math.round(io.github.ChanesCMD.trailcraft.climate.Climate.air(source.getLevel(), pos)),
                    Math.round(io.github.ChanesCMD.trailcraft.climate.Climate.mean(source.getLevel(), pos))), false);
            return 1;
        }));
    }
}

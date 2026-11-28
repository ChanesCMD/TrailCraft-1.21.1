package io.github.ChanesCMD.trailcraft.calendar;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Locale;

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

    /** /calendar — год, сезон, день и время; /calendar set <год> <сезон> <день> — переставить дату. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("calendar")
                .executes(CalendarEvents::info)
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("year", LongArgumentType.longArg(Calendar.START_YEAR))
                                .then(Commands.argument("season", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (Calendar.Season season : Calendar.Season.values()) {
                                                builder.suggest(season.name().toLowerCase(Locale.ROOT));
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("day", IntegerArgumentType.integer(1, 24))
                                                .executes(CalendarEvents::setDate))))));
    }

    /** Показать текущую дату, настройки и температуру. */
    private static int info(CommandContext<CommandSourceStack> ctx) {
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
    }

    /** Переставить календарную дату, сохранив текущее время суток. */
    private static int setDate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int daysPerSeason = ServerConfig.daysPerSeason();
        long year = LongArgumentType.getLong(ctx, "year");
        int day = IntegerArgumentType.getInteger(ctx, "day");
        Calendar.Season season;
        try {
            season = Calendar.Season.valueOf(StringArgumentType.getString(ctx, "season").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Сезон: spring, summer, autumn, winter"));
            return 0;
        }
        if (day > daysPerSeason) {
            source.sendFailure(Component.literal("День: 1.." + daysPerSeason));
            return 0;
        }
        long targetDay = (year - Calendar.START_YEAR) * (4L * daysPerSeason)
                + (long) season.ordinal() * daysPerSeason + (day - 1);
        long inDay = Math.floorMod(source.getServer().overworld().getDayTime() + Calendar.MIDNIGHT_OFFSET,
                Calendar.TICKS_PER_DAY);
        long time = targetDay * Calendar.TICKS_PER_DAY + inDay - Calendar.MIDNIGHT_OFFSET;
        if (time < 0) {
            time = 0;
        }
        for (ServerLevel level : source.getServer().getAllLevels()) {
            level.setDayTime(time);
        }
        source.sendSuccess(() -> Component.translatable("commands.trailcraft.calendar.set",
                year, Component.translatable(season.translationKey()), day), true);
        return 1;
    }
}

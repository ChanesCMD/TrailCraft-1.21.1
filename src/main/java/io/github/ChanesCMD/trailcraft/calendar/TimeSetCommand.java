package io.github.ChanesCMD.trailcraft.calendar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /time set} меняет только время суток, а день остаётся прежним: календарь (Calendar) считает дату по dayTime,
 * и ванильная команда, ставящая dayTime целиком, отбрасывала бы дату к первому дню.
 * Brigadier при повторной регистрации того же пути заменяет у узла только обработчик, так что подсказки
 * и права ванильной команды сохраняются.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class TimeSetCommand {
    private static final long DAY = 24000L;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("time").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.literal("day").executes(c -> set(c.getSource(), 1000)))
                        .then(Commands.literal("noon").executes(c -> set(c.getSource(), 6000)))
                        .then(Commands.literal("night").executes(c -> set(c.getSource(), 13000)))
                        .then(Commands.literal("midnight").executes(c -> set(c.getSource(), 18000)))
                        .then(Commands.argument("time", TimeArgument.time())
                                .executes(c -> set(c.getSource(), IntegerArgumentType.getInteger(c, "time"))))));
    }

    /** Время суток {@code time} в текущем дне каждого измерения. */
    private static int set(CommandSourceStack source, int time) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            // день календаря начинается в полночь (Calendar.MIDNIGHT_OFFSET): время ставится внутри него
            long shift = Calendar.MIDNIGHT_OFFSET;
            level.setDayTime(Math.floorDiv(level.getDayTime() + shift, DAY) * DAY + Math.floorMod(time + shift, DAY) - shift);
        }
        source.sendSuccess(() -> Component.translatable("commands.time.set", time), true);
        return (int) Math.floorMod(source.getLevel().getDayTime(), DAY);
    }

    private TimeSetCommand() {}
}

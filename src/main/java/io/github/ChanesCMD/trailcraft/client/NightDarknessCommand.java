package io.github.ChanesCMD.trailcraft.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Клиентская команда /trailcraft nightDarkness [0..100] для тестовой тёмной ночи. */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class NightDarknessCommand {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("trailcraft")
                .then(Commands.literal("nightDarkness")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> message(ClientConfig.NIGHT_DARKNESS.get()), false);
                            return 1;
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    ClientConfig.NIGHT_DARKNESS.set(value);
                                    ClientConfig.SPEC.save();
                                    ctx.getSource().sendSuccess(() -> message(value), false);
                                    return 1;
                                }))));
    }

    private static Component message(int value) {
        return Component.translatable("commands.trailcraft.night_darkness", value);
    }

    private NightDarknessCommand() {}
}

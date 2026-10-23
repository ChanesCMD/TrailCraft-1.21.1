package io.github.ChanesCMD.trailcraft.stamina;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Выносливость считается на сервере и отправляется клиенту (полоска в окне рюкзака, запрет бега на нуле). */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class StaminaEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        Stamina.Tracker tracker = player.getData(ModAttachments.STAMINA_TRACKER);
        float stamina = player.getData(ModAttachments.STAMINA);
        if (player.isSprinting()) {
            stamina -= Stamina.SPRINT_COST;
            tracker.cooldown = Stamina.REGEN_DELAY;
        } else if (tracker.cooldown > 0) {
            tracker.cooldown--;
        } else {
            stamina += Stamina.REGEN;
        }
        set(player, stamina);
        if (player.getData(ModAttachments.STAMINA) <= 0) {
            player.setSprinting(false);
        }
    }

    /** Прыжок игрока сервер тоже обрабатывает (по пакету движения), поэтому событие приходит и здесь. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            player.getData(ModAttachments.STAMINA_TRACKER).cooldown = Stamina.REGEN_DELAY;
            set(player, player.getData(ModAttachments.STAMINA) - Stamina.JUMP_COST);
        }
    }

    private static void set(ServerPlayer player, float value) {
        float stamina = Mth.clamp(value, 0, Stamina.MAX);
        player.setData(ModAttachments.STAMINA, stamina);
        Stamina.Tracker tracker = player.getData(ModAttachments.STAMINA_TRACKER);
        boolean edge = (stamina == 0 || stamina == Stamina.MAX) && stamina != tracker.lastSent;
        if (edge || Math.abs(stamina - tracker.lastSent) >= 1) {
            tracker.lastSent = stamina;
            PacketDistributor.sendToPlayer(player, new Stamina.SyncPayload(stamina));
        }
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.STAMINA);
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Stamina.SyncPayload.TYPE, Stamina.SyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> context.player().setData(ModAttachments.STAMINA, payload.value())));
        }
    }
}

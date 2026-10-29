package io.github.ChanesCMD.trailcraft.water;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModEffects;
import io.github.ChanesCMD.trailcraft.inventory.BeltHandler;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Вода считается на сервере и отправляется клиенту (капли в HUD). См. Water. */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class WaterEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        Water.Tracker tracker = player.getData(ModAttachments.WATER_TRACKER);
        if (tracker.drinkCooldown > 0) {
            tracker.drinkCooldown--;
        }
        if (player.isInWater()) {
            BeltHandler.of(player).fillFlask(Water.isSalty(player.level(), player.blockPosition()));
        }
        int water = get(player);
        Difficulty difficulty = player.level().getDifficulty();
        if (difficulty != Difficulty.PEACEFUL) {
            tracker.thirst += Water.THIRST_PER_TICK + (player.isSprinting() ? Water.SPRINT_THIRST_PER_TICK : 0);
            MobEffectInstance salt = player.getEffect(ModEffects.SALT_THIRST);
            if (salt != null) {
                tracker.thirst += Water.SALT_THIRST_PER_TICK * (salt.getAmplifier() + 1);
            }
            while (tracker.thirst >= 1) {
                tracker.thirst -= 1;
                water--;
            }
        }
        if (water <= 0 && ++tracker.damageTimer >= Water.DAMAGE_INTERVAL) {
            tracker.damageTimer = 0;
            float health = player.getHealth();
            if (health > 10 || difficulty == Difficulty.HARD || health > 1 && difficulty == Difficulty.NORMAL) {
                player.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(Water.THIRST_DAMAGE)), 1);
            }
        } else if (water > 0) {
            tracker.damageTimer = 0;
        }
        set(player, water);
    }

    /**
     * ПКМ пустой рукой по воде (в пределах досягаемости, ближе твёрдых блоков): глоток, если запас не полный,
     * и фляга на поясе наполняется доверху — даже при полном запасе воды (если в ней нет воды другого вида).
     * Морская вода накладывает «Солёную жажду».
     */
    private static void drink(ServerPlayer player) {
        Water.Tracker tracker = player.getData(ModAttachments.WATER_TRACKER);
        if (player.isSpectator() || !player.getMainHandItem().isEmpty() || tracker.drinkCooldown > 0) {
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1, true);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !player.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
            return;
        }
        boolean salty = Water.isSalty(player.level(), blockHit.getBlockPos());
        ItemStack flask = BeltHandler.of(player).flask();
        boolean flaskNeedsWater = !flask.isEmpty() && FlaskItem.canFill(flask, salty);
        if (get(player) >= Water.MAX && !flaskNeedsWater) {
            return;
        }
        tracker.drinkCooldown = Water.DRINK_COOLDOWN;
        if (get(player) < Water.MAX) {
            set(player, get(player) + Water.DRINK_AMOUNT);
            playDrinkSound(player);
            if (salty) {
                saltThirst(player);
            }
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
                    player.getSoundSource(), 1f, 1f);
        }
        BeltHandler.of(player).fillFlask(salty);
    }

    /**
     * Глоток из бочки с водой (block/WaterBarrelBlock): как из воды в мире, но без фляги. false — пить нельзя (полный запас,
     * перерыв между глотками).
     */
    public static boolean drinkFromBarrel(ServerPlayer player, boolean salty) {
        Water.Tracker tracker = player.getData(ModAttachments.WATER_TRACKER);
        if (player.isSpectator() || tracker.drinkCooldown > 0 || get(player) >= Water.MAX) {
            return false;
        }
        tracker.drinkCooldown = Water.DRINK_COOLDOWN;
        set(player, get(player) + Water.DRINK_AMOUNT);
        playDrinkSound(player);
        if (salty) {
            saltThirst(player);
        }
        return true;
    }

    private static void saltThirst(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.SALT_THIRST, Water.SALT_DURATION));
    }

    /** Глоток из фляги на поясе: фляга не пустая, запас воды не полный. */
    private static void drinkFromFlask(ServerPlayer player) {
        Water.Tracker tracker = player.getData(ModAttachments.WATER_TRACKER);
        if (player.isSpectator() || tracker.drinkCooldown > 0 || get(player) >= Water.MAX) {
            return;
        }
        BeltHandler belt = BeltHandler.of(player);
        int sips = FlaskItem.water(belt.flask());
        if (sips <= 0) {
            return;
        }
        boolean salty = FlaskItem.isSalty(belt.flask());
        belt.setFlaskWater(sips - 1);
        tracker.drinkCooldown = Water.DRINK_COOLDOWN;
        set(player, get(player) + Water.DRINK_AMOUNT);
        playDrinkSound(player);
        if (salty) {
            saltThirst(player);
        }
    }

    /** Вылить всю воду из фляги на поясе (в никуда). */
    private static void pourFlask(ServerPlayer player) {
        BeltHandler belt = BeltHandler.of(player);
        if (player.isSpectator() || FlaskItem.water(belt.flask()) <= 0) {
            return;
        }
        belt.setFlaskWater(0);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_EMPTY,
                player.getSoundSource(), 0.6f, 1.2f);
    }

    private static void playDrinkSound(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK,
                player.getSoundSource(), 0.5f, player.getRandom().nextFloat() * 0.1f + 0.9f);
    }

    private static void set(ServerPlayer player, int value) {
        int water = Mth.clamp(value, 0, Water.MAX);
        player.setData(ModAttachments.WATER, water);
        Water.Tracker tracker = player.getData(ModAttachments.WATER_TRACKER);
        if (water != tracker.lastSent) {
            tracker.lastSent = water;
            PacketDistributor.sendToPlayer(player, new Water.SyncPayload(water));
        }
    }

    public static int get(Player player) {
        return player.getData(ModAttachments.WATER);
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToClient(Water.SyncPayload.TYPE, Water.SyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> context.player().setData(ModAttachments.WATER, payload.value())));
            registrar.playToServer(Water.DrinkPayload.TYPE, Water.DrinkPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            drink(player);
                        }
                    }));
            registrar.playToServer(Water.FlaskPourPayload.TYPE, Water.FlaskPourPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            pourFlask(player);
                        }
                    }));
            registrar.playToServer(Water.FlaskDrinkPayload.TYPE, Water.FlaskDrinkPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            drinkFromFlask(player);
                        }
                    }));
        }
    }
}

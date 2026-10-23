package io.github.ChanesCMD.trailcraft.spear;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModEnchantments;
import io.github.ChanesCMD.trailcraft.init.ModTiers;
import io.github.ChanesCMD.trailcraft.item.SpearItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Бой копьём на сервере: укол (SpearJabPayload) и разгон (SpearItem.onUseTick). См. SpearItem. */
public final class SpearCombat {
    /** Где был разгоняющийся в прошлый тик: по разнице считаем его скорость (скорость игрока сервер не знает). */
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    /** Кого и когда (игровой тик) разгон уже задел. */
    private static final Map<UUID, Map<UUID, Long>> CONTACTS = new HashMap<>();

    /** Укол: все цели на линии взгляда в пределах досягаемости получают урон копья. */
    public static void jab(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        // Клиент колет только при полной зарядке; сервер чуть терпимее из-за задержки сети
        if (!(stack.getItem() instanceof SpearItem) || player.isUsingItem() || player.isSpectator()
                || player.getAttackStrengthScale(0.5f) < 0.9f) {
            return;
        }
        player.resetAttackStrengthTicker();
        ServerLevel level = player.serverLevel();
        // Толчок «Выпада» делает клиент (движение игрока считает он), сервер берёт плату
        int lunge = lungeLevel(player, stack);
        if (lunge > 0) {
            if (!player.getAbilities().instabuild) {
                player.causeFoodExhaustion(ModTiers.LUNGE_EXHAUSTION * lunge);
            }
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1, 1.3f);
            if (stack.isEmpty()) {
                return;
            }
        }
        DamageSource source = player.damageSources().playerAttack(player);
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hit = false;
        for (Entity target : targets(player)) {
            float damage = EnchantmentHelper.modifyDamage(level, stack, target, source, base);
            if (!target.hurt(source, damage)) {
                continue;
            }
            hit = true;
            float knockback = EnchantmentHelper.modifyKnockback(level, stack, target, source,
                    (float) player.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
            if (knockback > 0 && target instanceof LivingEntity living) {
                Vec3 look = player.getLookAngle();
                living.knockback(knockback * 0.5f, -look.x, -look.z);
            }
            EnchantmentHelper.doPostAttackEffects(level, target, source);
            player.setLastHurtMob(target);
        }
        if (hit) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.causeFoodExhaustion(0.1f);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                hit ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_NODAMAGE, player.getSoundSource(), 1, 1);
    }

    /** Тик разгона на сервере. ticksUsed — сколько тиков держится ПКМ. */
    public static void chargeTick(LivingEntity user, ItemStack stack, SpearItem spear, int ticksUsed) {
        Vec3 pos = user.position();
        Vec3 last = LAST_POS.put(user.getUUID(), pos);
        Vec3 velocity = last == null ? Vec3.ZERO : pos.subtract(last);
        int stage = spear.stage(ticksUsed);
        if (stage == 0 || stage == 4) {
            return;
        }
        ModTiers.SpearStats stats = spear.stats();
        Level level = user.level();
        if (ticksUsed == SpearItem.ticks(stats.delay())) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), user.getSoundSource(), 0.8f, 1.2f);
        }
        Vec3 look = user.getLookAngle();
        long now = level.getGameTime();
        Map<UUID, Long> contacts = CONTACTS.computeIfAbsent(user.getUUID(), id -> new HashMap<>());
        for (Entity target : targets(user)) {
            Long lastHit = contacts.get(target.getUUID());
            if (lastHit != null && now - lastHit < ModTiers.SPEAR_CONTACT_COOLDOWN) {
                continue;
            }
            // Относительная скорость вдоль взгляда, блоков в секунду
            double speed = velocity.subtract(velocityOf(target)).dot(look) * 20;
            if (speed < ModTiers.SPEAR_DAMAGE_SPEED) {
                continue;
            }
            Vec3 motion = target.getDeltaMovement();
            if (!target.hurt(source(user), (float) (stats.chargeMultiplier() * speed))) {
                continue;
            }
            contacts.put(target.getUUID(), now);
            boolean knockback = stage <= 2 && speed >= ModTiers.SPEAR_KNOCKBACK_SPEED;
            if (!knockback) {
                target.setDeltaMovement(motion); // hurt() сам отбрасывает — на третьей стадии отменяем
            } else if (target instanceof LivingEntity living) {
                living.knockback(0.4 + speed * 0.06, -look.x, -look.z);
            }
            if (stage == 1 && speed >= stats.dismountSpeed()) {
                target.stopRiding();
            }
            if (user instanceof Player player) {
                player.setLastHurtMob(target);
            }
            stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
            if (stack.isEmpty()) {
                break;
            }
        }
    }

    /**
     * Уровень «Выпада» для укола или 0, если он сейчас не сработает: верхом, в полёте на элитрах, в воде
     * или когда еды меньше LUNGE_MIN_FOOD (в творческом еда не нужна).
     */
    public static int lungeLevel(Player player, ItemStack stack) {
        if (player.isPassenger() || player.isFallFlying() || player.isInWater()
                || !player.getAbilities().instabuild && player.getFoodData().getFoodLevel() < ModTiers.LUNGE_MIN_FOOD) {
            return 0;
        }
        return player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ModEnchantments.LUNGE)
                .map(lunge -> EnchantmentHelper.getItemEnchantmentLevel(lunge, stack)).orElse(0);
    }

    /** Толчок «Выпада»: по горизонтали, тем сильнее, чем ровнее смотрит игрок. */
    public static Vec3 lungeImpulse(Player player, int level) {
        Vec3 look = player.getLookAngle();
        return new Vec3(look.x, 0, look.z).scale(ModTiers.LUNGE_IMPULSE * level);
    }

    public static void stopCharge(LivingEntity user) {
        LAST_POS.remove(user.getUUID());
        CONTACTS.remove(user.getUUID());
    }

    private static DamageSource source(LivingEntity user) {
        return user instanceof Player player ? user.damageSources().playerAttack(player) : user.damageSources().mobAttack(user);
    }

    /** Скорость цели за тик. Игроки двигаются на клиенте: если цель тоже разгоняется, берём её отслеживаемую скорость. */
    private static Vec3 velocityOf(Entity target) {
        if (target instanceof Player) {
            Vec3 last = LAST_POS.get(target.getUUID());
            return last == null ? Vec3.ZERO : target.position().subtract(last);
        }
        return target.getDeltaMovement();
    }

    /** Все, кого копьё достаёт по линии взгляда: дальше SPEAR_MIN_REACH, ближе SPEAR_MAX_REACH и первого твёрдого блока. */
    private static List<Entity> targets(LivingEntity user) {
        Level level = user.level();
        Vec3 eye = user.getEyePosition();
        Vec3 end = eye.add(user.getLookAngle().scale(ModTiers.SPEAR_MAX_REACH));
        // Трава, паутина и прочее без коллизии копью не мешают
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        double limit = eye.distanceTo(end);
        List<Entity> result = new ArrayList<>();
        for (Entity entity : level.getEntities(user, new AABB(eye, end).inflate(1), SpearCombat::canHit)) {
            if (entity == user.getVehicle() || user.isPassengerOfSameVehicle(entity) || entity.skipAttackInteraction(user)) {
                continue;
            }
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + ModTiers.SPEAR_HITBOX_MARGIN);
            double distance;
            if (box.contains(eye)) {
                distance = 0;
            } else {
                Optional<Vec3> hit = box.clip(eye, end);
                if (hit.isEmpty()) {
                    continue;
                }
                distance = eye.distanceTo(hit.get());
            }
            if (distance >= ModTiers.SPEAR_MIN_REACH && distance <= limit) {
                result.add(entity);
            }
        }
        return result;
    }

    private static boolean canHit(Entity entity) {
        return entity.isPickable() && entity.isAttackable() && !entity.isSpectator() && entity.isAlive();
    }

    private SpearCombat() {}

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(SpearJabPayload.TYPE, SpearJabPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            jab(player);
                        }
                    }));
        }
    }
}

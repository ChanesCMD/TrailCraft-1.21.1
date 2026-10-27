package io.github.ChanesCMD.trailcraft.spear;

import io.github.ChanesCMD.trailcraft.init.ModEntities;
import io.github.ChanesCMD.trailcraft.item.ThrowingSpearItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Брошенное метательное копьё. Как трезубец: попав в существо, наносит урон своего материала (ThrowingSpearItem.damage),
 * отскакивает и падает; лежит, пока его не подберут (не исчезает), и возвращается в инвентарь с прежней прочностью.
 * Предмет синхронизируется клиенту — по нему рисуется копьё нужного материала (client/ThrownSpearRenderer).
 */
public class ThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);
    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> type, Level level) {
        super(type, level);
    }

    public ThrownSpear(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.THROWN_SPEAR.get(), owner, level, stack, null);
        entityData.set(ITEM, stack.copy());
        if (stack.getItem() instanceof ThrowingSpearItem spear) {
            setBaseDamage(spear.damage());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ITEM, ItemStack.EMPTY);
    }

    /** Копьё, как оно выглядит (для отрисовки). */
    public ItemStack getItem() {
        return entityData.get(ITEM);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return getItem().isEmpty() ? ItemStack.EMPTY : getItem().copy();
    }

    @Override
    public void tick() {
        if (inGround) {
            dealtDamage = true;
        }
        super.tick();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = damageSources().trident(this, owner == null ? this : owner);
        dealtDamage = true;
        if (target.hurt(source, (float) getBaseDamage()) && target instanceof LivingEntity living) {
            doPostHurtEffects(living);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.0f);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    /** Лежит, пока не подберут (как трезубец); брошенное в творческом режиме исчезает, как стрела. */
    @Override
    protected void tickDespawn() {
        if (pickup != Pickup.ALLOWED) {
            super.tickDespawn();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("dealt_damage", dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dealtDamage = tag.getBoolean("dealt_damage");
        entityData.set(ITEM, getPickupItemStackOrigin().copy());
    }
}

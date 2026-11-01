package io.github.ChanesCMD.trailcraft.cushion;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

/**
 * Подушка для сидения (бэкпорт cushion из Minecraft 26.3). Это сущность, а не блок: ПКМ — сесть, удар — подобрать
 * (в выживании выпадает предмет). Без коллизии, шириной в блок и плоская (4 пикселя), может перекрываться с блоками и любыми объектами, кроме других подушек.
 * Как в 26.3, ставится по центру блока и не поворачивается; по высоте — в точку клика с шагом в пиксель (1/16 блока)
 * (CushionItem). Ломается, только если под ней не осталось опоры (в том числе когда она частично внутри блока).
 */
public class CushionEntity extends Entity {
    /** Плоская, во весь блок: 16×4 пикселя (как в 26.3: ковёр-подушка на целую клетку). */
    public static final float WIDTH = 1f, HEIGHT = 4 / 16f;
    /** На сколько ниже подушки ещё ищем опору: слой снега или ковёр под ней не должны её ломать. */
    private static final double SUPPORT_REACH = 3 / 16.0;
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(CushionEntity.class, EntityDataSerializers.INT);

    public CushionEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public DyeColor getColor() {
        return DyeColor.byId(entityData.get(DATA_COLOR));
    }

    public void setColor(DyeColor color) {
        entityData.set(DATA_COLOR, color.getId());
    }

    public ItemStack getItemStack() {
        return new ItemStack(ModItems.CUSHIONS.get(getColor()).get());
    }

    /** Есть ли под подушкой (или в ней самой) блок с коллизией. */
    public static boolean isSupported(Level level, Entity self, AABB box) {
        return level.getBlockCollisions(self, box.expandTowards(0, -SUPPORT_REACH, 0)).iterator().hasNext();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, DyeColor.WHITE.getId());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setColor(DyeColor.byId(tag.getInt("Color")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Color", getColor().getId());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return getItemStack();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || !getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRemoved() || level().isClientSide || !(source.getEntity() instanceof Player player)) {
            return false;
        }
        pickUp(!player.getAbilities().instabuild);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 10 == 0 && !isSupported(level(), this, getBoundingBox())) {
            pickUp(true);
        }
    }

    /** Убирает подушку из мира; drop — выронить предмет. */
    private void pickUp(boolean drop) {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
        gameEvent(GameEvent.ENTITY_DIE);
        if (drop) {
            spawnAtLocation(getItemStack());
        }
        discard();
    }
}

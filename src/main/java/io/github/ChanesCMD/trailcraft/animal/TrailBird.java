package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.sounds.SoundEvents;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Курица, гусь, утка, индюк: как ванильная курица, корм — #trailcraft:<id>_food, яйца несёт только курица. */
public class TrailBird extends Chicken {
    public TrailBird(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public Species species() {
        return Species.of(getType());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(species().food);
    }

    /** Ванильное яйцо курица не несёт (eggTime на время aiStep — заведомо большой), своё — по своему счётчику. */
    @Override
    public void aiStep() {
        int ownEggTime = eggTime;
        eggTime = Integer.MAX_VALUE;
        super.aiStep();
        eggTime = ownEggTime;
        if (species() != Species.CHICKEN) {
            eggTime = Integer.MAX_VALUE;
        } else if (!level().isClientSide && isAlive() && !isBaby() && !isChickenJockey() && --eggTime <= 0) {
            playSound(SoundEvents.CHICKEN_EGG, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f);
            spawnAtLocation(ModItems.EGG.get());
            gameEvent(GameEvent.ENTITY_PLACE);
            eggTime = random.nextInt(6000) + 6000;
        }
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Breeding.child(this, level);
    }

    /** Гусь и индюк крупнее курицы — голос ниже. */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (species() == Species.GOOSE || species() == Species.TURKEY ? 0.75f : 1);
    }
}

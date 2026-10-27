package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Волк: ванильный (стая, приручение костью), свой тип и модель; дикий охотится на овец, коз, зайцев и лис TrailCraft. */
public class TrailWolf extends Wolf {
    public TrailWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Ванильный волк выбирает добычу по ванильным типам — своих овец, зайцев и лис не видит
        targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Animal.class, false,
                target -> target instanceof Sheep || target instanceof Rabbit || target instanceof Fox || target instanceof Goat));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Wolf child = Breeding.child(this, level);
        if (child != null && isTame()) {
            child.setOwnerUUID(getOwnerUUID());
            child.setTame(true, true);
        }
        return child;
    }
}

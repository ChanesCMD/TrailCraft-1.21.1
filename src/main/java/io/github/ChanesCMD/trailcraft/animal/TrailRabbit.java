package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Заяц: ванильный кролик (прыгает, ест морковь с грядок), свой тип и модель. */
public class TrailRabbit extends Rabbit {
    public TrailRabbit(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Rabbit getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Rabbit child = Breeding.child(this, level);
        if (child != null) {
            child.setVariant(getVariant());
        }
        return child;
    }
}

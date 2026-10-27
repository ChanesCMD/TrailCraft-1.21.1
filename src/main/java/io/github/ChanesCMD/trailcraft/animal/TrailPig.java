package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Свинья: ванильное поведение, свой тип, модель и корм (#trailcraft:<id>_food вдобавок к ванильному). */
public class TrailPig extends Pig {
    public TrailPig(EntityType<? extends Pig> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Pig getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Breeding.child(this, level);
    }
}

package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Коза: ванильное поведение, свой тип, модель и корм (#trailcraft:<id>_food вдобавок к ванильному). */
public class TrailGoat extends Goat {
    public TrailGoat(EntityType<? extends Goat> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Goat getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Breeding.child(this, level);
    }
}

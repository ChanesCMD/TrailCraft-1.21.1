package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Манул (на основе оцелота: сторонится людей, охотится на птиц): ванильное поведение, свой тип, модель и корм (#trailcraft:<id>_food вдобавок к ванильному). */
public class TrailOcelot extends Ocelot {
    public TrailOcelot(EntityType<? extends Ocelot> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Ocelot getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Breeding.child(this, level);
    }
}

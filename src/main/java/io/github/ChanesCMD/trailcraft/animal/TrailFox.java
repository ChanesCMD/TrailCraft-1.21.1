package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Лиса, песец, фенек: ванильная лиса (спит днём, охотится на птиц и зайцев, таскает вещи), свой тип и модель. */
public class TrailFox extends Fox {
    public TrailFox(EntityType<? extends Fox> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Fox getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Fox child = Breeding.child(this, level);
        if (child != null) {
            child.setVariant(getVariant());
        }
        return child;
    }
}

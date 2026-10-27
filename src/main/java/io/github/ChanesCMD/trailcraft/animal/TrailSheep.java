package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Овца: ванильная (стрижка, шерсть и лут по цвету), свой тип и модель. Ванильная овца отращивает шерсть, только
 * съев ванильную траву; здесь шерсть ещё и сама отрастает, пока овца стоит на земле (почвы — в #minecraft:dirt).
 */
public class TrailSheep extends Sheep {
    /** Шерсть отрастает на земле в среднем за столько тиков (5 минут). */
    private static final int REGROW_TICKS = 6000;

    public TrailSheep(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isSheared() && random.nextInt(REGROW_TICKS) == 0
                && level().getBlockState(blockPosition().below()).is(BlockTags.DIRT)) {
            ate();
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return super.isFood(stack) || stack.is(Species.of(getType()).food);
    }

    @Override
    public Sheep getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Sheep child = Breeding.child(this, level);
        if (child != null && partner instanceof Sheep other) {
            child.setColor(random.nextBoolean() ? getColor() : other.getColor());
        }
        return child;
    }
}

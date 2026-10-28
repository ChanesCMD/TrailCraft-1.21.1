package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

/** Волк: ванильный (стая, приручение костью — своей, trailcraft:bone), свой тип и модель; дикий охотится на овец, коз, зайцев и лис TrailCraft. */
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

    /** Приручается своей костью, как ванильный волк ванильной (шанс 1 из 3). */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.BONE.get()) || isTame() || isAngry()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        stack.consume(1, player);
        if (random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
            tame(player);
            navigation.stop();
            setTarget(null);
            setOrderedToSit(true);
            level().broadcastEntityEvent(this, (byte) 7);
        } else {
            level().broadcastEntityEvent(this, (byte) 6);
        }
        return InteractionResult.SUCCESS;
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

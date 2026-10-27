package io.github.ChanesCMD.trailcraft.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Истощение игрока для полоски в HUD (health/FoodSync). */
@Mixin(FoodData.class)
public interface FoodDataAccessor {
    @Accessor("exhaustionLevel")
    float trailcraft$getExhaustion();
}

package io.github.ChanesCMD.trailcraft.water;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * «Солёная жажда» — как ванильный «Голод», только для воды: пока действует, вода тратится быстрее
 * (Water.SALT_THIRST_PER_TICK за уровень, считает WaterEvents). Накладывается, если выпить морскую воду.
 */
public class SaltThirstEffect extends MobEffect {
    public SaltThirstEffect() {
        super(MobEffectCategory.HARMFUL, 0x5E8CE8);
    }
}

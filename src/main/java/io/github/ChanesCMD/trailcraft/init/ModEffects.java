package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.water.SaltThirstEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TrailCraft.MODID);

    /** Морская вода: вода тратится быстрее, как еда при «Голоде». */
    public static final DeferredHolder<MobEffect, MobEffect> SALT_THIRST = EFFECTS.register("salt_thirst", SaltThirstEffect::new);

    private ModEffects() {}
}

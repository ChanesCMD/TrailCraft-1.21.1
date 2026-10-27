package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Частицы погоды (рисует и спавнит клиент, client/weather/). Текстуры — scripts/gen_weather_textures.py. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TrailCraft.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_DROP = register("rain_drop");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNOW_FLAKE = register("snow_flake");
    /** Брызги от капли на земле; скорость при спавне — цвет (r, g, b от 0 до 1). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_SPLASH = register("rain_splash");
    /** Круг на воде от капли; скорость при спавне — цвет. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_RIPPLE = register("rain_ripple");
    /** Пыль пыльной бури; скорость при спавне — цвет земли. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_MOTE = register("dust_mote");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        // overrideLimiter: частицы видны и дальше 32 блоков, плотность погода считает сама
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(true) {});
    }

    private ModParticles() {}
}

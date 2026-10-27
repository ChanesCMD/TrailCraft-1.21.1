package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.animal.Capercaillie;
import io.github.ChanesCMD.trailcraft.spear.ThrownSpear;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, TrailCraft.MODID);

    /** Брошенное метательное копьё (как трезубец: летит, бьёт, остаётся лежать и подбирается). */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownSpear>> THROWN_SPEAR = ENTITY_TYPES.register("thrown_spear",
            () -> EntityType.Builder.<ThrownSpear>of(ThrownSpear::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .clientTrackingRange(4).updateInterval(20).build("thrown_spear"));

    /** Глухарь (animal/Capercaillie): как курица, но без яиц. */
    public static final DeferredHolder<EntityType<?>, EntityType<Capercaillie>> CAPERCAILLIE = ENTITY_TYPES.register("capercaillie",
            () -> EntityType.Builder.<Capercaillie>of(Capercaillie::new, MobCategory.CREATURE).sized(0.6f, 0.95f).eyeHeight(0.8f)
                    .clientTrackingRange(10).build("capercaillie"));

    private ModEntities() {}
}

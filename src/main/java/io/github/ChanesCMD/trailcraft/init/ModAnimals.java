package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.animal.FlyingBird;
import io.github.ChanesCMD.trailcraft.animal.Species;
import io.github.ChanesCMD.trailcraft.animal.TrailAnimal;
import io.github.ChanesCMD.trailcraft.animal.TrailBird;
import io.github.ChanesCMD.trailcraft.animal.TrailCow;
import io.github.ChanesCMD.trailcraft.animal.TrailFox;
import io.github.ChanesCMD.trailcraft.animal.TrailGoat;
import io.github.ChanesCMD.trailcraft.animal.TrailOcelot;
import io.github.ChanesCMD.trailcraft.animal.TrailPig;
import io.github.ChanesCMD.trailcraft.animal.TrailRabbit;
import io.github.ChanesCMD.trailcraft.animal.TrailSheep;
import io.github.ChanesCMD.trailcraft.animal.TrailWolf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.Map;

/** Регистрация животных из Species (в ModEntities.ENTITY_TYPES). */
public final class ModAnimals {
    public static final Map<Species, DeferredHolder<EntityType<?>, EntityType<? extends Animal>>> TYPES = new EnumMap<>(Species.class);

    static {
        for (Species species : Species.values()) {
            TYPES.put(species, ModEntities.ENTITY_TYPES.register(species.id, () -> {
                EntityType<? extends Animal> type = builder(species.kind).sized(species.width, species.height)
                        .clientTrackingRange(10).build(species.id);
                Species.bind(type, species);
                return type;
            }));
        }
    }

    private static EntityType.Builder<? extends Animal> builder(Species.Kind kind) {
        return switch (kind) {
            case BIRD -> EntityType.Builder.<TrailBird>of(TrailBird::new, MobCategory.CREATURE);
            case FLYING -> EntityType.Builder.<FlyingBird>of(FlyingBird::new, MobCategory.CREATURE);
            case PIG -> EntityType.Builder.<TrailPig>of(TrailPig::new, MobCategory.CREATURE);
            case COW -> EntityType.Builder.<TrailCow>of(TrailCow::new, MobCategory.CREATURE);
            case SHEEP -> EntityType.Builder.<TrailSheep>of(TrailSheep::new, MobCategory.CREATURE);
            case GOAT -> EntityType.Builder.<TrailGoat>of(TrailGoat::new, MobCategory.CREATURE);
            case RABBIT -> EntityType.Builder.<TrailRabbit>of(TrailRabbit::new, MobCategory.CREATURE);
            case FOX -> EntityType.Builder.<TrailFox>of(TrailFox::new, MobCategory.CREATURE);
            case WOLF -> EntityType.Builder.<TrailWolf>of(TrailWolf::new, MobCategory.CREATURE);
            case OCELOT -> EntityType.Builder.<TrailOcelot>of(TrailOcelot::new, MobCategory.CREATURE);
            case ANIMAL -> EntityType.Builder.<TrailAnimal>of(TrailAnimal::new, MobCategory.CREATURE);
        };
    }

    public static EntityType<? extends Animal> type(Species species) {
        return TYPES.get(species).get();
    }

    /** Чтобы статический блок выполнился до регистрации (вызывается из TrailCraft). */
    public static void init() {}

    private ModAnimals() {}
}

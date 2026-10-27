package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAnimals;
import io.github.ChanesCMD.trailcraft.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Животные TrailCraft: атрибуты и правила появления (в каких биомах — spawners в JSON биомов, scripts/gen_animals.py). */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AnimalEvents {
    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CAPERCAILLIE.get(), Capercaillie.createAttributes().build());
        for (Species species : Species.values()) {
            AttributeSupplier.Builder base = switch (species.kind) {
                case BIRD -> Chicken.createAttributes();
                case FLYING -> FlyingBird.createAttributes();
                case PIG -> Pig.createAttributes();
                case COW -> Cow.createAttributes();
                case SHEEP -> Sheep.createAttributes();
                case GOAT -> Goat.createAttributes();
                case RABBIT -> Rabbit.createAttributes();
                case FOX -> Fox.createAttributes();
                case WOLF -> Wolf.createAttributes();
                case OCELOT -> Ocelot.createAttributes();
                case ANIMAL -> TrailAnimal.createAttributes();
            };
            event.put(ModAnimals.type(species), base.add(Attributes.MAX_HEALTH, species.health).build());
        }
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.CAPERCAILLIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Capercaillie::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (Species species : Species.values()) {
            register(event, ModAnimals.type(species));
        }
    }

    private static <T extends Entity> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AnimalEvents::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * Ванильные животные появляются только на ванильной траве (свои тэги *_spawnable_on), а земля TrailCraft — свои
     * почвы, пески, породы и снег. Где кому жить, решают биомы; здесь — только твёрдая опора (ON_GROUND) и свет.
     */
    private static <T extends Entity> boolean checkSpawnRules(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                              BlockPos pos, RandomSource random) {
        return level.getRawBrightness(pos, 0) > 8;
    }
}

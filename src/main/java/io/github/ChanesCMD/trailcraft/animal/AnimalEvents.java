package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Животные TrailCraft: атрибуты и правила появления (в каких биомах — spawners в JSON биомов). */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AnimalEvents {
    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CAPERCAILLIE.get(), Capercaillie.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.CAPERCAILLIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Capercaillie::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}

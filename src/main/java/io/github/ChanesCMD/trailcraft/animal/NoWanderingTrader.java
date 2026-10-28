package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Странствующий торговец сам не приходит: его и его лам ванильный WanderingTraderSpawner ставит с типом EVENT — такое
 * появление отменяется (из яйца призыва и командой они по-прежнему появляются).
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class NoWanderingTrader {
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        EntityType<?> type = event.getEntity().getType();
        if (event.getSpawnType() == MobSpawnType.EVENT
                && (type == EntityType.WANDERING_TRADER || type == EntityType.TRADER_LLAMA)) {
            event.setSpawnCancelled(true);
        }
    }
}

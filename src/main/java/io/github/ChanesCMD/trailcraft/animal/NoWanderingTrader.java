package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Странствующий торговец (с ламами), разбойники и фантомы сами не появляются: ванильные WanderingTraderSpawner,
 * PatrolSpawner (патрули и рейды), заставы разбойников и PhantomSpawner ставят их с «автоматическим» типом появления —
 * такое появление отменяется. Из яйца призыва, командой, из спавнера и т. п. они по-прежнему появляются.
 * Отменённых помечаем тегом и не пускаем в мир и на join (на случай, если отмена finalizeSpawn мобу не мешает).
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class NoWanderingTrader {
    private static final String BLOCKED_TAG = "trailcraft_no_spawn";
    private static final Set<MobSpawnType> AUTOMATIC = EnumSet.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.STRUCTURE, MobSpawnType.PATROL, MobSpawnType.EVENT, MobSpawnType.REINFORCEMENT);

    private static boolean blocked(EntityType<?> type) {
        return type == EntityType.WANDERING_TRADER || type == EntityType.TRADER_LLAMA
                || type == EntityType.PILLAGER || type == EntityType.PHANTOM;
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (blocked(event.getEntity().getType()) && AUTOMATIC.contains(event.getSpawnType())) {
            event.getEntity().addTag(BLOCKED_TAG);
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity().getTags().contains(BLOCKED_TAG)) {
            event.setCanceled(true);
        }
    }
}

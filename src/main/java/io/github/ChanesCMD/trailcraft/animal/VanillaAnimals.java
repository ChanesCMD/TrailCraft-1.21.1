package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAnimals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;

/**
 * Ванильных курицы, свиньи, коровы, овцы, козы, кролика, лисы и волка в мире нет: любой из них (из яйца, ванильного
 * яйца призыва, старого сохранения) при появлении заменяется своим видом с теми же данными — классы TrailCraft
 * наследуют ванильные, поэтому NBT подходит (возраст, цвет шерсти, хозяин волка и т. д.).
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public class VanillaAnimals {
    private static final Map<EntityType<?>, Species> REPLACED = Map.of(
            EntityType.CHICKEN, Species.CHICKEN, EntityType.PIG, Species.PIG, EntityType.COW, Species.COW,
            EntityType.SHEEP, Species.SHEEP, EntityType.GOAT, Species.GOAT, EntityType.RABBIT, Species.HARE,
            EntityType.FOX, Species.FOX, EntityType.WOLF, Species.WOLF);

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Species species = REPLACED.get(entity.getType());
        if (species == null || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof Fox fox && fox.getVariant() == Fox.Type.SNOW) {
            species = Species.ARCTIC_FOX;
        }
        event.setCanceled(true);
        Entity replacement = ModAnimals.type(species).create(level);
        if (replacement == null) {
            return;
        }
        replacement.load(entity.saveWithoutId(new CompoundTag()));
        // Добавлять сущность прямо во время добавления другой (загрузка чанка) нельзя — на следующем шаге сервера
        level.getServer().execute(() -> level.addFreshEntity(replacement));
    }
}

package io.github.ChanesCMD.trailcraft.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Детёныш своего вида: ванильные классы создают детёныша ванильного типа (EntityType.PIG и т. п.). */
final class Breeding {
    @SuppressWarnings("unchecked")
    static <T extends Entity> T child(Entity parent, ServerLevel level) {
        return (T) parent.getType().create(level);
    }

    private Breeding() {}
}

package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Свои звуки (файлы — assets/trailcraft/sounds.json и sounds/). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, TrailCraft.MODID);

    /** Глухарь: квохтанье и ток (scripts/gen_capercaillie_sounds.py). */
    public static final DeferredHolder<SoundEvent, SoundEvent> CAPERCAILLIE_AMBIENT = register("entity.capercaillie.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAPERCAILLIE_SONG = register("entity.capercaillie.song");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, name)));
    }

    private ModSounds() {}
}

package io.github.ChanesCMD.trailcraft.init;

import com.mojang.serialization.Codec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TrailCraft.MODID);

    /** Дополнительные слоты игрока. После смерти копируются (без keepInventory они к этому моменту уже выброшены). */
    public static final Supplier<AttachmentType<PlayerSlots>> PLAYER_SLOTS = ATTACHMENT_TYPES.register("player_slots",
            () -> AttachmentType.serializable(() -> new PlayerSlots()).copyOnDeath().build());

    /** Переключатели косметики: бит i = показывать косметику вместо брони в слоте i (0 — шлем … 3 — ботинки). */
    public static final Supplier<AttachmentType<Integer>> COSMETIC_TOGGLES = ATTACHMENT_TYPES.register("cosmetic_toggles",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    private ModAttachments() {}
}

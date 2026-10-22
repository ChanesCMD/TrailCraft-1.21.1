package io.github.ChanesCMD.trailcraft.init;

import com.mojang.serialization.Codec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(TrailCraft.MODID);

    /** Сколько раз ветку уже потёрли о породу (0..2), хранится на стаке. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUBS =
            COMPONENTS.registerComponentType("rubs", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModComponents() {}
}

package io.github.ChanesCMD.trailcraft.init;

import com.mojang.serialization.Codec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(TrailCraft.MODID);

    /** Сколько раз ветку уже потёрли о породу (0..2), хранится на стаке. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUBS =
            COMPONENTS.registerComponentType("rubs", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Запас воды во фляге, в глотках (0..Water.FLASK_SIPS). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATER =
            COMPONENTS.registerComponentType("water", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Содержимое рюкзака, кошелька или пояса: предметы лежат в самом предмете и уходят вместе с ним. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CONTENTS =
            COMPONENTS.registerComponentType("contents", builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    private ModComponents() {}
}

package io.github.ChanesCMD.trailcraft.init;

import com.mojang.serialization.Codec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.smelting.Molten;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import net.minecraft.core.GlobalPos;
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

    /** Во фляге морская вода (нет компонента — пресная). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SALTY =
            COMPONENTS.registerComponentType("salty", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Содержимое рюкзака, кошелька или пояса: предметы лежат в самом предмете и уходят вместе с ним. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> CONTENTS =
            COMPONENTS.registerComponentType("contents", builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    /** Линейка: первый отмеченный блок (с измерением), от него меряется расстояние до второго. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> RULER_START =
            COMPONENTS.registerComponentType("ruler_start", builder -> builder.persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC));

    /** Жидкий металл в кувшине (после плавки в малой сыродутной печи). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Molten>> MOLTEN =
            COMPONENTS.registerComponentType("molten", builder -> builder.persistent(Molten.CODEC)
                    .networkSynchronized(Molten.STREAM_CODEC));

    /** Какой металл залит в форму (нет компонента — форма пустая). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Metal>> CAST =
            COMPONENTS.registerComponentType("cast", builder -> builder.persistent(Metal.CODEC)
                    .networkSynchronized(Metal.STREAM_CODEC));

    private ModComponents() {}
}

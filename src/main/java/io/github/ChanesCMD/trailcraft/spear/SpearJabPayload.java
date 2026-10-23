package io.github.ChanesCMD.trailcraft.spear;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент → сервер: игрок уколол копьём (ЛКМ). Цели сервер ищет сам. */
public record SpearJabPayload() implements CustomPacketPayload {
    public static final SpearJabPayload INSTANCE = new SpearJabPayload();
    public static final Type<SpearJabPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "spear_jab"));
    public static final StreamCodec<ByteBuf, SpearJabPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

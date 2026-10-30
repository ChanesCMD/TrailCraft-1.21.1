package io.github.ChanesCMD.trailcraft.season;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Сервер → клиент: сметённые метлой столбцы рядом с игроком (x, y — где лёг бы снег, z); клиент подсвечивает их, пока метла в руке. */
public record SweptSnowPayload(List<BlockPos> columns) implements CustomPacketPayload {
    public static final Type<SweptSnowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "swept_snow"));
    public static final StreamCodec<ByteBuf, SweptSnowPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SweptSnowPayload::columns, SweptSnowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

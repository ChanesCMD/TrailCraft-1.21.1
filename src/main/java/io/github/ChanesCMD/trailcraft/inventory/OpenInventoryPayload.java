package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент → сервер: игрок нажал E, открыть инвентарь TrailCraft вместо ванильного. */
public record OpenInventoryPayload() implements CustomPacketPayload {
    public static final OpenInventoryPayload INSTANCE = new OpenInventoryPayload();
    public static final Type<OpenInventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "open_inventory"));
    public static final StreamCodec<ByteBuf, OpenInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

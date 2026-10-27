package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Клиент → сервер: игрок нажал E, открыть инвентарь TrailCraft вместо ванильного. В креативе инвентарь TrailCraft
 * открывается только плюсом у слота кошелька, пояса или рюкзака (creative = true), а E остаётся ванильной.
 */
public record OpenInventoryPayload(boolean creative) implements CustomPacketPayload {
    public static final OpenInventoryPayload INSTANCE = new OpenInventoryPayload(false);
    public static final OpenInventoryPayload FROM_CREATIVE = new OpenInventoryPayload(true);
    public static final Type<OpenInventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "open_inventory"));
    public static final StreamCodec<ByteBuf, OpenInventoryPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, OpenInventoryPayload::creative, OpenInventoryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

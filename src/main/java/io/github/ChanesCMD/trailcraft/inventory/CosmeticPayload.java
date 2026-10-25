package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Сервер → клиенты: какую косметику показывать на игроке вместо брони. stacks — 4 предмета (шлем … ботинки),
 * пустой — показывать надетую броню.
 */
public record CosmeticPayload(int entityId, List<ItemStack> stacks) implements CustomPacketPayload {
    public static final Type<CosmeticPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "cosmetic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CosmeticPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(4)), CosmeticPayload::stacks,
            CosmeticPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

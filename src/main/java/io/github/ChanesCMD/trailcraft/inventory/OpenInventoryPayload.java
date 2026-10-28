package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Клиент → сервер: игрок нажал E, открыть инвентарь TrailCraft вместо ванильного. В креативе E открывает креативное
 * окно, а инвентарь TrailCraft — его вкладка «Инвентарь» (creative = true); carried — предмет, который креатив держал
 * на курсоре (останется на курсоре).
 */
public record OpenInventoryPayload(boolean creative, ItemStack carried) implements CustomPacketPayload {
    public static final OpenInventoryPayload INSTANCE = new OpenInventoryPayload(false, ItemStack.EMPTY);
    public static final Type<OpenInventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "open_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenInventoryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenInventoryPayload::creative,
            ItemStack.OPTIONAL_STREAM_CODEC, OpenInventoryPayload::carried,
            OpenInventoryPayload::new);

    public static OpenInventoryPayload fromCreative(ItemStack carried) {
        return new OpenInventoryPayload(true, carried);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

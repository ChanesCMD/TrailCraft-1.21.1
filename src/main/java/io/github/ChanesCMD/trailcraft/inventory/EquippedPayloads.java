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
 * Слоты кошелька, пояса и рюкзака во вкладке «Инвентарь» креатива. Креатив пользуется ванильным меню, где этих
 * слотов нет, поэтому сервер присылает креативному игроку, что в них надето (Sync), а клиент сообщает, что туда
 * положили (Set) — как ванильный креатив сообщает о своих слотах.
 */
public final class EquippedPayloads {
    /** Слоты PlayerSlots в порядке показа: кошелёк, пояс, рюкзак (как в нижнем ряду инвентаря). */
    public static final int[] SLOTS = {PlayerSlots.WALLET, PlayerSlots.BELT, PlayerSlots.BACKPACK};

    /** Сервер → клиент: предметы в слотах SLOTS. */
    public record Sync(List<ItemStack> stacks) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "equipped_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC =
                StreamCodec.composite(ItemStack.OPTIONAL_LIST_STREAM_CODEC, Sync::stacks, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер (только в креативе): положить предмет в слот PlayerSlots (пустой стак — снять). */
    public record Set(int slot, ItemStack stack) implements CustomPacketPayload {
        public static final Type<Set> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "equipped_set"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Set> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Set::slot, ItemStack.OPTIONAL_STREAM_CODEC, Set::stack, Set::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private EquippedPayloads() {}
}

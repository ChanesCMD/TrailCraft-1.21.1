package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Сервер → клиент: что лежит в надетом рюкзаке и на поясе из того, что нужно HUD и кнопкам (атлас, календарь,
 * фляга, приборы для координат).
 * Слоты игрока сами на клиент не синхронизируются, а HUD работает и при закрытом инвентаре.
 */
public record GearPayload(int flags) implements CustomPacketPayload {
    /**
     * FLASK — на поясе фляга, и она не полная (можно набрать воды ПКМ даже с полной полоской воды).
     * ALTIMETER, SEXTANT, CHRONOMETER, GPS — приборы на поясе (см. client/Coords).
     */
    public static final int ATLAS = 1, CALENDAR = 2, FLASK = 4, ALTIMETER = 8, SEXTANT = 16, CHRONOMETER = 32, GPS = 64;
    public static final Type<GearPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "gear"));
    public static final StreamCodec<ByteBuf, GearPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, GearPayload::flags, GearPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

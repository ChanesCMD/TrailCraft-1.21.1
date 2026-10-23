package io.github.ChanesCMD.trailcraft.stamina;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Выносливость: тратится при беге и прыжках, после короткой паузы восстанавливается.
 * На нуле бег невозможен. Все числа черновые. TODO: уточнить.
 */
public final class Stamina {
    public static final float MAX = 100;
    /** Расход за тик бега: полный запас — примерно 20 секунд бега. */
    public static final float SPRINT_COST = 0.25f;
    public static final float JUMP_COST = 4;
    /** Восстановление за тик (полный запас — примерно 10 секунд). */
    public static final float REGEN = 0.5f;
    /** Сколько тиков после траты выносливость не восстанавливается. */
    public static final int REGEN_DELAY = 30;

    /** Изменения на сервере, которые ещё не отправлены клиенту (не сохраняются). */
    public static class Tracker {
        public int cooldown;
        public float lastSent = -1;
    }

    /** Сервер → клиент: текущее значение выносливости. */
    public record SyncPayload(float value) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "stamina"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.FLOAT, SyncPayload::value, SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Stamina() {}
}

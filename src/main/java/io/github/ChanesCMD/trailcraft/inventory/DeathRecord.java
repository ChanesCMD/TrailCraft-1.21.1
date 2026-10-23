package io.github.ChanesCMD.trailcraft.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Запись журнала смертей. time — реальное время смерти (мс), оно же связывает запись с могилой.
 * dayTime — время суток мира для игровой даты. pos — где стоит могила (или где умер, если могилы нет).
 */
public record DeathRecord(long time, long dayTime, String dimension, BlockPos pos, boolean hadBackpack, boolean taken) {
    /** Сколько реальных миллисекунд могила доступна только владельцу. */
    public static final long OWNER_ONLY_MS = 2 * 60 * 60 * 1000L;
    /** Сколько последних смертей хранится. */
    public static final int MAX_RECORDS = 50;

    public static final Codec<DeathRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("time").forGetter(DeathRecord::time),
            Codec.LONG.fieldOf("day_time").forGetter(DeathRecord::dayTime),
            Codec.STRING.fieldOf("dimension").forGetter(DeathRecord::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(DeathRecord::pos),
            Codec.BOOL.fieldOf("had_backpack").forGetter(DeathRecord::hadBackpack),
            Codec.BOOL.fieldOf("taken").forGetter(DeathRecord::taken)
    ).apply(i, DeathRecord::new));

    public enum BackpackStatus {
        /** Рюкзака при смерти не было. */
        NONE,
        /** Владелец забрал рюкзак из могилы. */
        TAKEN,
        /** Прошло 2 часа, владелец не забрал — могилу мог открыть кто угодно. */
        UNKNOWN,
        /** Меньше 2 часов, рюкзак ещё в могиле. */
        PRESENT;

        public String translationKey() {
            return "gui.trailcraft.deaths.backpack." + name().toLowerCase();
        }
    }

    public BackpackStatus status(long now) {
        if (!hadBackpack) return BackpackStatus.NONE;
        if (taken) return BackpackStatus.TAKEN;
        return now - time >= OWNER_ONLY_MS ? BackpackStatus.UNKNOWN : BackpackStatus.PRESENT;
    }

    public static void add(Player player, DeathRecord record) {
        List<DeathRecord> list = new ArrayList<>(player.getData(ModAttachments.DEATH_LOG));
        list.add(record);
        while (list.size() > MAX_RECORDS) {
            list.remove(0);
        }
        player.setData(ModAttachments.DEATH_LOG, list);
    }

    /** Владелец забрал рюкзак из могилы, созданной в момент time. */
    public static void markTaken(Player player, long time) {
        List<DeathRecord> list = new ArrayList<>(player.getData(ModAttachments.DEATH_LOG));
        list.replaceAll(r -> r.time == time && r.hadBackpack ? new DeathRecord(r.time, r.dayTime, r.dimension, r.pos, true, true) : r);
        player.setData(ModAttachments.DEATH_LOG, list);
    }
}

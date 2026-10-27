package io.github.ChanesCMD.trailcraft.water;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Вода (жажда) работает как голод: тратится со временем, быстрее при беге и прыжках; на нуле — урон, как от голода.
 * Пьют ПКМ пустой рукой по воде или из фляги на поясе. Все числа черновые. TODO: уточнить.
 */
public final class Water {
    /** Полный запас: 10 капель по 2 единицы, как у еды. */
    public static final int MAX = 20;
    /**
     * Жажда за тик просто так и дополнительно за тик бега; каждая накопленная единица жажды отнимает единицу воды.
     * Вода тратится вдвое медленнее, чем в первой версии.
     */
    public static final float THIRST_PER_TICK = 1f / 2400, SPRINT_THIRST_PER_TICK = 1f / 600;
    /** Жажда за прыжок. */
    public static final float JUMP_THIRST = 0.025f;
    /** Сколько воды даёт один глоток (из воды и из фляги) и сколько тиков между глотками. */
    public static final int DRINK_AMOUNT = 4, DRINK_COOLDOWN = 10;
    /** Выше этого запаса воды бег тратит вдвое меньше выносливости (Stamina.SPRINT_COST_WATERED). */
    public static final int WELL_WATERED = MAX * 7 / 10;
    /** Фляга на поясе: сколько в ней глотков (каждый — DRINK_AMOUNT воды). */
    public static final int FLASK_SIPS = 10;
    /** На нуле: урон 1 раз в столько тиков (как от голода). */
    public static final int DAMAGE_INTERVAL = 80;

    public static final ResourceKey<DamageType> THIRST_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "thirst"));

    /** Состояние на сервере, которое не сохраняется. */
    public static class Tracker {
        public float thirst;
        public int damageTimer;
        public int drinkCooldown;
        public int lastSent = -1;
    }

    /** Сервер → клиент: текущий запас воды. */
    public record SyncPayload(int value) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "water"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncPayload::value, SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: игрок хочет попить из воды перед собой. Сервер проверяет сам. */
    public record DrinkPayload() implements CustomPacketPayload {
        public static final DrinkPayload INSTANCE = new DrinkPayload();
        public static final Type<DrinkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "drink"));
        public static final StreamCodec<ByteBuf, DrinkPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: глоток из фляги на поясе (кнопка «Выпить»). */
    public record FlaskDrinkPayload() implements CustomPacketPayload {
        public static final FlaskDrinkPayload INSTANCE = new FlaskDrinkPayload();
        public static final Type<FlaskDrinkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "flask_drink"));
        public static final StreamCodec<ByteBuf, FlaskDrinkPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Water() {}
}

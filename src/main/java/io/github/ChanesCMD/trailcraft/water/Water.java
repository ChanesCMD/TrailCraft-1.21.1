package io.github.ChanesCMD.trailcraft.water;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Вода (жажда) работает как голод: тратится со временем, быстрее при беге (прыжки воду не тратят); на нуле — урон, как от голода.
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
    /** Сколько воды даёт один глоток (из воды и из фляги) и сколько тиков между глотками. */
    public static final int DRINK_AMOUNT = 4, DRINK_COOLDOWN = 10;
    /**
     * Морская вода (биомы #trailcraft:salt_water — море и его берега) утоляет жажду как пресная, но накладывает
     * «Солёную жажду» (ModEffects.SALT_THIRST) на SALT_DURATION тиков: за тик и уровень эффекта — SALT_THIRST_PER_TICK жажды.
     */
    public static final int SALT_DURATION = 600;
    public static final float SALT_THIRST_PER_TICK = 1f / 400;
    public static final TagKey<Biome> SALT_WATER = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "salt_water"));
    /** Выше этого запаса воды бег тратит вдвое меньше выносливости (Stamina.SPRINT_COST_WATERED). */
    public static final int WELL_WATERED = MAX * 7 / 10;
    /** Фляга на поясе: сколько в ней глотков (каждый — DRINK_AMOUNT воды). */
    public static final int FLASK_SIPS = 10;
    /** На нуле: урон 1 раз в столько тиков (как от голода). */
    public static final int DAMAGE_INTERVAL = 80;

    /** Вода в этом месте морская (по биому). */
    public static boolean isSalty(Level level, BlockPos pos) {
        return level.getBiome(pos).is(SALT_WATER);
    }

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

    /** Клиент → сервер: вылить воду из фляги на поясе (кнопка «Вылить»). */
    public record FlaskPourPayload() implements CustomPacketPayload {
        public static final FlaskPourPayload INSTANCE = new FlaskPourPayload();
        public static final Type<FlaskPourPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "flask_pour"));
        public static final StreamCodec<ByteBuf, FlaskPourPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

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

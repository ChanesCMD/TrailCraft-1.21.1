package io.github.ChanesCMD.trailcraft.init;

import com.mojang.serialization.Codec;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.inventory.BackpackHandler;
import io.github.ChanesCMD.trailcraft.inventory.BeltHandler;
import io.github.ChanesCMD.trailcraft.inventory.DeathRecord;
import io.github.ChanesCMD.trailcraft.inventory.PlayerSlots;
import io.github.ChanesCMD.trailcraft.stamina.Stamina;
import io.github.ChanesCMD.trailcraft.water.Water;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TrailCraft.MODID);

    /** Дополнительные слоты игрока. После смерти копируются (без keepInventory они к этому моменту уже выброшены). */
    public static final Supplier<AttachmentType<PlayerSlots>> PLAYER_SLOTS = ATTACHMENT_TYPES.register("player_slots",
            () -> AttachmentType.serializable(() -> new PlayerSlots()).copyOnDeath().build());

    /** Переключатели косметики: бит i = показывать косметику вместо брони в слоте i (0 — шлем … 3 — ботинки). */
    public static final Supplier<AttachmentType<Integer>> COSMETIC_TOGGLES = ATTACHMENT_TYPES.register("cosmetic_toggles",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    /** Журнал смертей игрока (последние DeathRecord.MAX_RECORDS). */
    public static final Supplier<AttachmentType<List<DeathRecord>>> DEATH_LOG = ATTACHMENT_TYPES.register("death_log",
            () -> AttachmentType.<List<DeathRecord>>builder(() -> List.of()).serialize(DeathRecord.CODEC.listOf()).copyOnDeath().build());

    /** Сколько раз игрок умер за всё время (журнал хранит только последние записи). */
    public static final Supplier<AttachmentType<Integer>> DEATH_COUNT = ATTACHMENT_TYPES.register("death_count",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    /** Хотбар, отложенный в момент смерти: после возрождения возвращается игроку. */
    public static final Supplier<AttachmentType<ItemStackHandler>> KEPT_HOTBAR = ATTACHMENT_TYPES.register("kept_hotbar",
            () -> AttachmentType.serializable(() -> new ItemStackHandler(9)).build());

    /** Заметки к дням календаря: «год/сезон/день» → текст (см. CalendarNotes). */
    public static final Supplier<AttachmentType<Map<String, String>>> CALENDAR_NOTES = ATTACHMENT_TYPES.register("calendar_notes",
            () -> AttachmentType.<Map<String, String>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.STRING)).copyOnDeath().build());

    /** Политые лейкой грядки чанка: позиция блока (BlockPos.asLong) → время мира (dayTime), до которого грядка влажная. */
    public static final Supplier<AttachmentType<Map<Long, Long>>> WATERED_BEDS = ATTACHMENT_TYPES.register("watered_beds",
            () -> AttachmentType.<Map<Long, Long>>builder(() -> new HashMap<>())
                    .serialize(Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG)
                            .xmap(map -> (Map<Long, Long>) new HashMap<>(map), map -> map)).build());

    /** Породы и почвы чанка, поставленные игроком (BlockPos.asLong): они не обваливаются (collapse/PlacedBlocks). */
    public static final Supplier<AttachmentType<Set<Long>>> PLACED_BLOCKS = ATTACHMENT_TYPES.register("placed_blocks",
            () -> AttachmentType.<Set<Long>>builder(() -> new HashSet<>())
                    .serialize(Codec.LONG.listOf().xmap(list -> (Set<Long>) new HashSet<>(list), set -> List.copyOf(set))).build());

    /** Столбцы чанка, где метлой смели снег (BlockPos.asLong с y = 0): новый снег там не ложится (season/SweptSnow). */
    public static final Supplier<AttachmentType<Set<Long>>> SWEPT_SNOW = ATTACHMENT_TYPES.register("swept_snow",
            () -> AttachmentType.<Set<Long>>builder(() -> new HashSet<>())
                    .serialize(Codec.LONG.listOf().xmap(list -> (Set<Long>) new HashSet<>(list), set -> List.copyOf(set))).build());

    /** Последнее, что отправлено клиенту в GearPayload (не сохраняется: после входа отправится заново). */
    public static final Supplier<AttachmentType<Integer>> GEAR_SENT = ATTACHMENT_TYPES.register("gear_sent",
            () -> AttachmentType.builder(() -> -1).build());

    /** Слоты надетого рюкзака и пояса для серверных событий: создаются один раз на игрока (не сохраняются). */
    public static final Supplier<AttachmentType<BackpackHandler>> BACKPACK_HANDLER = ATTACHMENT_TYPES.register("backpack_handler",
            () -> AttachmentType.builder(holder -> new BackpackHandler((Player) holder)).build());
    public static final Supplier<AttachmentType<BeltHandler>> BELT_HANDLER = ATTACHMENT_TYPES.register("belt_handler",
            () -> AttachmentType.builder(holder -> new BeltHandler((Player) holder)).build());

    /** Последняя косметика, отправленная клиентам в CosmeticPayload (не сохраняется). */
    public static final Supplier<AttachmentType<List<ItemStack>>> COSMETIC_SENT = ATTACHMENT_TYPES.register("cosmetic_sent",
            () -> AttachmentType.<List<ItemStack>>builder(() -> List.of()).build());

    /** Выносливость (после смерти — снова полная). */
    public static final Supplier<AttachmentType<Float>> STAMINA = ATTACHMENT_TYPES.register("stamina",
            () -> AttachmentType.builder(() -> Stamina.MAX).serialize(Codec.FLOAT).build());

    /** Служебное состояние выносливости на сервере (не сохраняется). */
    public static final Supplier<AttachmentType<Stamina.Tracker>> STAMINA_TRACKER = ATTACHMENT_TYPES.register("stamina_tracker",
            () -> AttachmentType.builder(() -> new Stamina.Tracker()).build());

    /** Запас воды (после смерти — снова полный). */
    public static final Supplier<AttachmentType<Integer>> WATER = ATTACHMENT_TYPES.register("water",
            () -> AttachmentType.builder(() -> Water.MAX).serialize(Codec.INT).build());

    /** Служебное состояние воды на сервере (не сохраняется). */
    public static final Supplier<AttachmentType<Water.Tracker>> WATER_TRACKER = ATTACHMENT_TYPES.register("water_tracker",
            () -> AttachmentType.builder(() -> new Water.Tracker()).build());

    private ModAttachments() {}
}

package io.github.ChanesCMD.trailcraft.farm;

import net.minecraft.world.food.FoodProperties;

/**
 * Все числа земледелия в одном месте: скорость роста на грядках, ягодные кусты, ягоды.
 * TODO: уточнить — значения черновые.
 */
public final class Farm {
    // === ГРЯДКИ: скорость роста растений относительно обычной (ванильной) грядки ===
    /** Жёлтозём растит долго: вдвое медленнее. */
    public static final float ZHELTOZEM_GROWTH = 0.5f;
    /** Бурозём — как обычная грядка. */
    public static final float BUROZEM_GROWTH = 1.0f;
    /** Чернозём — лучше всех: в полтора раза быстрее. */
    public static final float CHERNOZEM_GROWTH = 1.5f;

    // === ЛЕЙКИ (item/WateringCanItem, farm/Watering) ===
    /** Политая грядка влажная столько игровых суток (сутки — 24000 тиков времени мира, при любой длине суток в конфиге). */
    public static final int WATERED_DAYS = 2;
    /** Поливает квадрат грядок со стороной 2 × WATERING_RADIUS + 1 вокруг грядки, по которой кликнули. */
    public static final int WATERING_RADIUS = 1;
    /** Сколько поливов вмещает лейка: деревянная, бронзовая, цинковая. */
    public static final int WOODEN_CAN_WATER = 8, BRONZE_CAN_WATER = 16, ZINC_CAN_WATER = 32;

    // === ЯГОДНЫЕ КУСТЫ (как сладкие ягоды) ===
    /** Куст подрастает (0 → 3) за случайный тик с шансом 1 из стольких. */
    public static final int BUSH_GROWTH_CHANCE = 5;
    /** ПКМ по кусту с ягодами: падает от BERRIES_MIN до BERRIES_MAX ягод, со спелого (стадия 3) — на одну больше. */
    public static final int BERRIES_MIN = 1, BERRIES_MAX = 2;
    /** Ягоды: сытость и насыщение — как у сладких ягод. */
    public static final FoodProperties BERRY_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build();

    private Farm() {}
}

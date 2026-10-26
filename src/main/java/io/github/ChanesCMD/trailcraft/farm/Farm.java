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

    // === ЯГОДНЫЕ КУСТЫ (как сладкие ягоды) ===
    /** Куст подрастает (0 → 3) за случайный тик с шансом 1 из стольких. */
    public static final int BUSH_GROWTH_CHANCE = 5;
    /** ПКМ по кусту с ягодами: падает от BERRIES_MIN до BERRIES_MAX ягод, со спелого (стадия 3) — на одну больше. */
    public static final int BERRIES_MIN = 1, BERRIES_MAX = 2;
    /** Ягоды: сытость и насыщение — как у сладких ягод. */
    public static final FoodProperties BERRY_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build();

    private Farm() {}
}

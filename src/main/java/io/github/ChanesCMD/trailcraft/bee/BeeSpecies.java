package io.github.ChanesCMD.trailcraft.bee;

import org.jetbrains.annotations.Nullable;

/**
 * Виды пчёл (пасека — ApiaryBlockEntity). Дикие (без родителей) живут в диких ульях своих биомов (biome_modifier
 * bees_<вид>.json, scripts/gen_bees.py); остальные выводятся в пасеке из двух родителей. Соты — id предмета сот,
 * combs — сколько сот за цикл, cycle — длина цикла в тиках.
 */
public enum BeeSpecies {
    FOREST("forest", 0x7A5230, "honey_comb", null, null, 1, 600),
    MEADOW("meadow", 0xE8B830, "honey_comb", null, null, 1, 600),
    STEPPE("steppe", 0xC89858, "honey_comb", null, null, 1, 600),
    SOUTHERN("southern", 0xE0782C, "honey_comb", null, null, 1, 600),
    MARSH("marsh", 0x627E3C, "honey_comb", null, null, 1, 600),
    HONEY("honey", 0xF5A01A, "honey_comb", FOREST, MEADOW, 2, 600),
    WAX("wax", 0xF0E2A8, "wax_comb", MEADOW, STEPPE, 1, 600),
    RESIN("resin", 0xB8621C, "resin_comb", FOREST, MARSH, 1, 600),
    SUGAR("sugar", 0xF6F2EA, "sugar_comb", SOUTHERN, HONEY, 1, 600),
    INDUSTRIOUS("industrious", 0x4A4A52, "honey_comb", HONEY, WAX, 1, 300);

    /** Шанс за цикл, что у двух пчёл, из которых выводится третий вид, появится он. */
    public static final float MUTATION_CHANCE = 0.12f;
    /** Шанс за цикл, что у двух пчёл появится ещё одна пчела вида одного из родителей. */
    public static final float OFFSPRING_CHANCE = 0.06f;

    public final String id;
    public final int color;
    public final String comb;
    @Nullable
    public final BeeSpecies parentA, parentB;
    public final int combs, cycle;

    BeeSpecies(String id, int color, String comb, @Nullable BeeSpecies parentA, @Nullable BeeSpecies parentB, int combs, int cycle) {
        this.id = id;
        this.color = color;
        this.comb = comb;
        this.parentA = parentA;
        this.parentB = parentB;
        this.combs = combs;
        this.cycle = cycle;
    }

    public boolean wild() {
        return parentA == null;
    }

    /** Какой вид выводится из этих двух (в любом порядке), или null. */
    @Nullable
    public static BeeSpecies mutation(BeeSpecies a, BeeSpecies b) {
        for (BeeSpecies s : values()) {
            if (!s.wild() && (s.parentA == a && s.parentB == b || s.parentA == b && s.parentB == a)) {
                return s;
            }
        }
        return null;
    }
}

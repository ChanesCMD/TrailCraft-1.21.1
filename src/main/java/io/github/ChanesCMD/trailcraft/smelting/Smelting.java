package io.github.ChanesCMD.trailcraft.smelting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Плавка в кувшине (как в TerraFirmaCraft). Каждый предмет в кувшине — одна единица металла (один слиток): кусок руды,
 * шихта или слиток. Шихта и слитки сплавов раскладываются на чистые металлы по своему составу. Потом весь кувшин
 * плавится разом: один металл — его слитки; доли металлов попали в состав сплава — слитки сплава (так сплав можно
 * собрать прямо в кувшине из кусков руды: 3 меди и 1 олово — 4 бронзы); иначе каждый металл отдельно (дробные
 * остатки пропадают). Железо, никель, литий и алюминий в малой сыродутной печи не плавятся — в кувшин не кладутся.
 */
public final class Smelting {
    /** Чистые металлы (то, на что раскладывается всё в кувшине) и сплавы. Слиток — id предмета. */
    public enum Metal {
        COPPER("minecraft:copper_ingot"), TIN("trailcraft:tin_ingot"), ZINC("create:zinc_ingot"),
        BISMUTH("trailcraft:bismuth_ingot"), LEAD("trailcraft:lead_ingot"), GOLD("minecraft:gold_ingot"),
        TIN_BRONZE("trailcraft:tin_bronze_ingot"), BISMUTH_BRONZE("trailcraft:bismuth_bronze_ingot"),
        BRASS("create:brass_ingot");

        private final ResourceLocation ingot;

        Metal(String ingot) {
            this.ingot = ResourceLocation.parse(ingot);
        }

        public Item ingot() {
            return BuiltInRegistries.ITEM.get(ingot);
        }
    }

    /** Сплав: доля каждого металла от … до … (больше ничего в кувшине быть не должно). */
    private record Alloy(Metal result, Map<Metal, double[]> parts) {}

    private static final List<Alloy> ALLOYS = List.of(
            new Alloy(Metal.TIN_BRONZE, Map.of(Metal.COPPER, new double[]{0.70, 0.80}, Metal.TIN, new double[]{0.20, 0.30})),
            new Alloy(Metal.BRASS, Map.of(Metal.COPPER, new double[]{0.70, 0.80}, Metal.ZINC, new double[]{0.20, 0.30})),
            new Alloy(Metal.BISMUTH_BRONZE, Map.of(Metal.COPPER, new double[]{0.45, 0.55},
                    Metal.BISMUTH, new double[]{0.20, 0.30}, Metal.ZINC, new double[]{0.20, 0.30})));

    /** Состав одной единицы предмета: металл → доля. */
    private static final Map<ResourceLocation, Map<Metal, Double>> MELTS = new HashMap<>();

    static {
        for (String id : List.of("trailcraft:raw_malachite", "trailcraft:raw_native_copper", "trailcraft:raw_tetrahedrite",
                "trailcraft:copper_mix", "minecraft:copper_ingot")) {
            pure(id, Metal.COPPER);
        }
        for (String id : List.of("trailcraft:raw_cassiterite", "trailcraft:tin_mix", "trailcraft:tin_ingot")) {
            pure(id, Metal.TIN);
        }
        for (String id : List.of("trailcraft:raw_sphalerite", "trailcraft:zinc_mix", "create:zinc_ingot")) {
            pure(id, Metal.ZINC);
        }
        for (String id : List.of("trailcraft:raw_native_bismuth", "trailcraft:bismuth_ingot")) {
            pure(id, Metal.BISMUTH);
        }
        for (String id : List.of("trailcraft:raw_galena", "trailcraft:lead_ingot")) {
            pure(id, Metal.LEAD);
        }
        for (String id : List.of("trailcraft:raw_native_gold", "minecraft:gold_ingot")) {
            pure(id, Metal.GOLD);
        }
        for (String id : List.of("trailcraft:tin_bronze_mix", "trailcraft:tin_bronze_ingot")) {
            MELTS.put(ResourceLocation.parse(id), Map.of(Metal.COPPER, 0.75, Metal.TIN, 0.25));
        }
        for (String id : List.of("trailcraft:brass_mix", "create:brass_ingot")) {
            MELTS.put(ResourceLocation.parse(id), Map.of(Metal.COPPER, 0.75, Metal.ZINC, 0.25));
        }
        for (String id : List.of("trailcraft:bismuth_bronze_mix", "trailcraft:bismuth_bronze_ingot")) {
            MELTS.put(ResourceLocation.parse(id), Map.of(Metal.COPPER, 0.5, Metal.BISMUTH, 0.25, Metal.ZINC, 0.25));
        }
    }

    private static void pure(String id, Metal metal) {
        MELTS.put(ResourceLocation.parse(id), Map.of(metal, 1.0));
    }

    /** Предметы, которые плавятся в чистый металл metal (для JEI). */
    public static List<Item> sources(Metal metal) {
        List<Item> items = new ArrayList<>();
        MELTS.forEach((id, parts) -> {
            if (parts.size() == 1 && parts.containsKey(metal)) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) {
                    items.add(item);
                }
            }
        });
        items.sort(java.util.Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return items;
    }

    /** Все предметы, которые кладутся в кувшин (для JEI). */
    public static List<Item> meltables() {
        List<Item> items = new ArrayList<>();
        for (ResourceLocation id : MELTS.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        items.sort(java.util.Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return items;
    }

    /** Плавится ли предмет в малой сыродутной печи (можно ли положить его в кувшин). */
    public static boolean melts(ItemStack stack) {
        return !stack.isEmpty() && MELTS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Сколько единиц металла (слитков) в содержимом. */
    public static int units(List<ItemStack> contents) {
        int n = 0;
        for (ItemStack stack : contents) {
            if (melts(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** Сколько тиков плавить столько единиц. */
    public static int time(int units) {
        return 200 + 40 * units;
    }

    /** Есть ли в содержимом кувшина то, что не плавится (мелочь): такой кувшин печь не плавит. */
    public static boolean hasForeign(List<ItemStack> contents) {
        return contents.stream().anyMatch(stack -> !stack.isEmpty() && !melts(stack));
    }

    /** Что выплавится из содержимого кувшина (пустой список — нечего плавить или в кувшине мелочь). */
    public static List<ItemStack> result(List<ItemStack> contents) {
        Map<Metal, Double> amounts = new EnumMap<>(Metal.class);
        int total = 0;
        if (hasForeign(contents)) {
            return new ArrayList<>();
        }
        for (ItemStack stack : contents) {
            if (!melts(stack)) {
                continue;
            }
            total += stack.getCount();
            MELTS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .forEach((metal, share) -> amounts.merge(metal, share * stack.getCount(), Double::sum));
        }
        List<ItemStack> out = new ArrayList<>();
        if (total == 0) {
            return out;
        }
        Metal single = null;
        for (Map.Entry<Metal, Double> e : amounts.entrySet()) {
            if (e.getValue() / total > 0.999) {
                single = e.getKey();
            }
        }
        if (single == null) {
            single = alloy(amounts, total);
        }
        if (single != null) {
            add(out, single.ingot(), total);
        } else {
            amounts.forEach((metal, amount) -> add(out, metal.ingot(), (int) Math.floor(amount + 1.0E-6)));
        }
        return out;
    }

    private static Metal alloy(Map<Metal, Double> amounts, int total) {
        for (Alloy alloy : ALLOYS) {
            if (!alloy.parts().keySet().containsAll(amounts.keySet())) {
                continue;
            }
            boolean fits = true;
            for (Map.Entry<Metal, double[]> part : alloy.parts().entrySet()) {
                double share = amounts.getOrDefault(part.getKey(), 0.0) / total;
                if (share < part.getValue()[0] - 1.0E-6 || share > part.getValue()[1] + 1.0E-6) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                return alloy.result();
            }
        }
        return null;
    }

    private static void add(List<ItemStack> out, Item item, int count) {
        if (item == Items.AIR) {
            return;
        }
        while (count > 0) {
            int n = Math.min(count, new ItemStack(item).getMaxStackSize());
            out.add(new ItemStack(item, n));
            count -= n;
        }
    }

    private Smelting() {}
}

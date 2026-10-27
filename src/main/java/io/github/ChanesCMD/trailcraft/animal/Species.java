package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Животные TrailCraft (кроме глухаря — у него свой класс). Здесь — как они устроены в игре: поведение (Kind), размер,
 * здоровье, цвета яйца призыва. Модели, текстуры, лут, корм (тэг #trailcraft:<id>_food), где живут и переводы пишет
 * scripts/gen_animals.py по такой же таблице — меняя вид, менять в обоих местах.
 */
public enum Species {
    CHICKEN("chicken", Kind.BIRD, 0.45f, 0.75f, 4, 0xB0602F, 0xD8402C),
    GOOSE("goose", Kind.BIRD, 0.6f, 1.1f, 8, 0x8C8276, 0xE08A2C),
    DUCK("duck", Kind.BIRD, 0.4f, 0.6f, 4, 0x7A6650, 0x2E6E3A),
    TURKEY("turkey", Kind.BIRD, 0.7f, 1.2f, 10, 0x463828, 0xC23030),
    CROW("crow", Kind.FLYING, 0.4f, 0.55f, 6, 0x96969A, 0x1E1E22),
    RAVEN("raven", Kind.FLYING, 0.5f, 0.65f, 8, 0x1E1E24, 0x3A3E52),
    PIG("pig", Kind.PIG, 0.9f, 0.9f, 10, 0xE2A096, 0xC48078),
    COW("cow", Kind.COW, 0.9f, 1.4f, 10, 0xE8E4DC, 0x2A2624),
    SHEEP("sheep", Kind.SHEEP, 0.9f, 1.3f, 8, 0xE8E6DE, 0xDCCDB4),
    GOAT("goat", Kind.GOAT, 0.7f, 1.3f, 10, 0xDCD8CE, 0x968A6E),
    HARE("hare", Kind.RABBIT, 0.45f, 0.55f, 4, 0x967652, 0xF0E8DC),
    CAPYBARA("capybara", Kind.ANIMAL, 0.9f, 0.9f, 12, 0x8C603C, 0x5E4028),
    BEAVER("beaver", Kind.ANIMAL, 0.6f, 0.55f, 10, 0x6E482C, 0x3C3632),
    FENNEC("fennec", Kind.FOX, 0.45f, 0.55f, 8, 0xE2C896, 0xF6E6C8),
    HEDGEHOG("hedgehog", Kind.ANIMAL, 0.45f, 0.45f, 6, 0x5A4632, 0xD2C8B4),
    MANUL("manul", Kind.OCELOT, 0.6f, 0.65f, 10, 0x968C7E, 0x3C3632),
    ARCTIC_FOX("arctic_fox", Kind.FOX, 0.6f, 0.7f, 10, 0xECECE8, 0xBCC4CE),
    FOX("fox", Kind.FOX, 0.6f, 0.7f, 10, 0xD66E28, 0xF4F0E8),
    WOLF("wolf", Kind.WOLF, 0.6f, 0.85f, 8, 0x82807C, 0x5C5A58);

    /** Поведение: на каком ванильном животном основан вид. */
    public enum Kind {
        /** Курица (TrailBird): бродит, хлопает крыльями, медленно падает; яйца несёт только курица. */
        BIRD,
        /** Летает (FlyingBird, как попугай), садится на землю. */
        FLYING,
        PIG, COW, SHEEP, GOAT, RABBIT, FOX, WOLF, OCELOT,
        /** Мирное животное без особенностей (TrailAnimal). */
        ANIMAL
    }

    private static final Map<EntityType<?>, Species> BY_TYPE = new HashMap<>();

    public final String id;
    public final Kind kind;
    public final float width, height;
    public final double health;
    public final int eggColor, eggSpots;
    /** Корм: приманивает и разводит (у видов на ванильной основе приманивает ещё и ванильный корм). */
    public final TagKey<Item> food;

    Species(String id, Kind kind, float width, float height, double health, int eggColor, int eggSpots) {
        this.id = id;
        this.kind = kind;
        this.width = width;
        this.height = height;
        this.health = health;
        this.eggColor = eggColor;
        this.eggSpots = eggSpots;
        this.food = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, id + "_food"));
    }

    public static void bind(EntityType<?> type, Species species) {
        BY_TYPE.put(type, species);
    }

    public static Species of(EntityType<?> type) {
        return BY_TYPE.get(type);
    }
}

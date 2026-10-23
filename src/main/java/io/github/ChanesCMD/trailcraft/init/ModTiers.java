package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Все числа инструментов в одном месте: уровни материалов и урон/скорость по типам предметов.
 * Значения черновые (ТЗ, часть 2, раздел 3). TODO: уточнить характеристики материалов.
 */
public final class ModTiers {
    /**
     * Что медь не добывает: то же, что и камень (железная руда и выше), но без брёвен,
     * поэтому медный топор рубит любые брёвна, а каменный/кремнёвый — только тонкие.
     */
    public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "incorrect_for_copper_tool"));

    // === УРОВНИ МАТЕРИАЛОВ: тег «не добывает», прочность, скорость, урон, зачаровываемость, ремонт ===
    // Кремень, кость и аметист добывают как камень (полные брёвна — нет, см. incorrect_for_stone_tool).
    public static final Tier FLINT = new SimpleTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 100, 4.0f, 1.0f, 5,
            () -> Ingredient.of(ModItems.FLINT.get()));
    public static final Tier BONE = new SimpleTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 120, 4.0f, 1.0f, 5,
            () -> Ingredient.of(Items.BONE));
    public static final Tier AMETHYST = new SimpleTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 160, 5.0f, 1.5f, 15,
            () -> Ingredient.of(Items.AMETHYST_SHARD));
    /** Медь: между камнем и железом. */
    public static final Tier COPPER = new SimpleTier(INCORRECT_FOR_COPPER_TOOL, 190, 5.0f, 1.5f, 5,
            () -> Ingredient.of(Items.COPPER_INGOT));
    // TODO: уточнить — бронзового и стального слитка пока нет, ремонт в наковальне отключён
    public static final Tier BRONZE = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 220, 5.5f, 2.0f, 12,
            () -> Ingredient.EMPTY);
    /** Сталь: между железом и алмазом. TODO: уточнить, добывает ли она блоки алмазного уровня. */
    public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 700, 7.0f, 2.5f, 14,
            () -> Ingredient.EMPTY);

    /** Материалы инструментов. Железо — ванильный уровень. Топоры у каждого материала свои (урон, скорость атаки). */
    public enum Material {
        FLINT("flint", ModTiers.FLINT, 7.0f, -3.2f),
        BONE("bone", ModTiers.BONE, 7.0f, -3.2f),
        AMETHYST("amethyst", ModTiers.AMETHYST, 7.0f, -3.2f),
        COPPER("copper", ModTiers.COPPER, 6.5f, -3.15f),
        BRONZE("bronze", ModTiers.BRONZE, 6.0f, -3.1f),
        IRON("iron", Tiers.IRON, 6.0f, -3.1f),
        STEEL("steel", ModTiers.STEEL, 5.5f, -3.0f);

        public final String id;
        public final Tier tier;
        public final float axeDamage, axeSpeed;

        Material(String id, Tier tier, float axeDamage, float axeSpeed) {
            this.id = id;
            this.tier = tier;
            this.axeDamage = axeDamage;
            this.axeSpeed = axeSpeed;
        }

        /** Металлы: из них делают серпы, косы, пилы и кусачки. */
        public boolean isMetal() {
            return this == COPPER || this == BRONZE || this == IRON || this == STEEL;
        }
    }

    // === УРОН И СКОРОСТЬ АТАКИ ПО ТИПАМ (прибавка к урону уровня и к базовой скорости 4.0) ===
    public static final float PICKAXE_DAMAGE = 1.0f, PICKAXE_SPEED = -2.8f;
    public static final float SHOVEL_DAMAGE = 1.5f, SHOVEL_SPEED = -3.0f;
    /** Нож слабее меча (у меча 3 и -2.4). */
    public static final int KNIFE_DAMAGE = 1;
    public static final float KNIFE_SPEED = -2.0f;
    public static final float SICKLE_DAMAGE = 1.5f, SICKLE_SPEED = -2.2f;
    public static final float SCYTHE_DAMAGE = 3.0f, SCYTHE_SPEED = -3.0f;
    public static final float SAW_DAMAGE = 1.0f, SAW_SPEED = -2.8f;
    public static final float CUTTERS_DAMAGE = 0.5f, CUTTERS_SPEED = -2.4f;
    /** Копьё бьёт дальше обычного на SPEAR_REACH блоков. TODO: уточнить механику копья. */
    public static final float SPEAR_DAMAGE = 3.0f, SPEAR_SPEED = -2.9f;
    public static final double SPEAR_REACH = 1.0;

    private ModTiers() {}
}

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
        // Копья: кремень и кость — как каменное копьё 1.21.11, медь и железо — как ванильные, сталь — как алмазное.
        FLINT("flint", ModTiers.FLINT, 7.0f, -3.2f, new SpearStats(2, 1.33f, 0.82f, 0.7f, 4.5f, 13, 9.0f, 13.75f)),
        BONE("bone", ModTiers.BONE, 7.0f, -3.2f, new SpearStats(2, 1.33f, 0.82f, 0.7f, 4.5f, 13, 9.0f, 13.75f)),
        AMETHYST("amethyst", ModTiers.AMETHYST, 7.0f, -3.2f, new SpearStats(2, 1.25f, 0.82f, 0.65f, 4.25f, 12.5f, 8.5f, 13.0f)),
        COPPER("copper", ModTiers.COPPER, 6.5f, -3.15f, new SpearStats(2, 1.18f, 0.82f, 0.65f, 4.0f, 12, 8.25f, 12.5f)),
        BRONZE("bronze", ModTiers.BRONZE, 6.0f, -3.1f, new SpearStats(3, 1.1f, 0.9f, 0.62f, 3.25f, 11.5f, 7.5f, 12.0f)),
        IRON("iron", Tiers.IRON, 6.0f, -3.1f, new SpearStats(3, 1.05f, 0.95f, 0.6f, 2.5f, 11, 6.75f, 11.25f)),
        STEEL("steel", ModTiers.STEEL, 5.5f, -3.0f, new SpearStats(4, 0.95f, 1.075f, 0.5f, 3.0f, 10, 6.5f, 10.0f));

        public final String id;
        public final Tier tier;
        public final float axeDamage, axeSpeed;
        public final SpearStats spear;

        Material(String id, Tier tier, float axeDamage, float axeSpeed, SpearStats spear) {
            this.id = id;
            this.tier = tier;
            this.axeDamage = axeDamage;
            this.axeSpeed = axeSpeed;
            this.spear = spear;
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

    // === КОПЬЁ (бэкпорт 1.21.11, см. SpearItem) ===
    /**
     * Числа копья одного материала. Время — в секундах.
     * @param jabDamage        урон укола (ЛКМ)
     * @param attackSpeed      скорость атаки (уколов в секунду при полной зарядке)
     * @param chargeMultiplier урон разгона = множитель × относительная скорость (блоков/с)
     * @param delay            сколько держать ПКМ, прежде чем копьё начнёт бить
     * @param dismountTime     сколько после этого разгон сбрасывает цель с седла (стадия 1)…
     * @param dismountSpeed    …если относительная скорость не меньше этой (блоков/с)
     * @param knockbackTime    до какого момента разгон отбрасывает цель (конец стадии 2)
     * @param damageTime       до какого момента разгон наносит урон (конец стадии 3, потом копьё опускается)
     */
    public record SpearStats(float jabDamage, float attackSpeed, float chargeMultiplier, float delay,
                             float dismountTime, float dismountSpeed, float knockbackTime, float damageTime) {}

    /** Укол и разгон достают цели от SPEAR_MIN_REACH до SPEAR_MAX_REACH блоков; хитбокс цели расширяется на поле. */
    public static final double SPEAR_MIN_REACH = 2.0, SPEAR_MAX_REACH = 4.5, SPEAR_HITBOX_MARGIN = 0.125;
    /** Минимальная относительная скорость (блоков/с), с которой разгон отбрасывает и ранит цель. */
    public static final float SPEAR_KNOCKBACK_SPEED = 5.1f, SPEAR_DAMAGE_SPEED = 4.6f;
    /** Одну и ту же цель разгон задевает не чаще раза в столько тиков. */
    public static final int SPEAR_CONTACT_COOLDOWN = 10;

    private ModTiers() {}
}

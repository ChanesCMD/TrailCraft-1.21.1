package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModEntities;
import io.github.ChanesCMD.trailcraft.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Глухарь: ведёт себя как курица (бродит, паникует, машет крыльями и медленно падает), но крупнее, ниже голосом,
 * яиц не несёт, приманивается и разводится смородиной (#trailcraft:capercaillie_food). Лут — перья и мясо
 * (loot_table/entities/capercaillie.json).
 */
public class Capercaillie extends Chicken {
    /** Ток — один раз из стольких звуков взрослого; длина тока в тиках. */
    private static final int SONG_CHANCE = 5, SONG_TICKS = 130;
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "capercaillie_food"));
    /** Мясо — как курица: сырое иногда даёт голод. */
    public static final FoodProperties RAW_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f)
            .effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3f).build();
    public static final FoodProperties COOKED_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build();

    public Capercaillie(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    /** Цели курицы, только корм свой (у курицы он зашит в TemptGoal). */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    /** Курица сносит яйцо, когда eggTime доходит до нуля: глухарю он не даёт дойти. */
    @Override
    public void aiStep() {
        eggTime = Integer.MAX_VALUE;
        super.aiStep();
    }

    @Override
    public Capercaillie getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CAPERCAILLIE.get().create(level);
    }

    /** Квохтанье; взрослый иногда токует (длинная песня — щелчки, «пробка», точение). */
    @Override
    protected SoundEvent getAmbientSound() {
        return !isBaby() && random.nextInt(SONG_CHANCE) == 0 ? ModSounds.CAPERCAILLIE_SONG.get() : ModSounds.CAPERCAILLIE_AMBIENT.get();
    }

    /** Ток длится 6 с: пока он не кончился, глухарь не квохчет поверх. */
    @Override
    public void playAmbientSound() {
        SoundEvent sound = getAmbientSound();
        if (sound == ModSounds.CAPERCAILLIE_SONG.get()) {
            ambientSoundTime = -SONG_TICKS;
        }
        // свои звуки уже низкие — без понижения getVoicePitch
        playSound(sound, getSoundVolume(), super.getVoicePitch());
    }

    /** Вскрик и смерть пока куриные — ниже голосом. */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7f;
    }

    /** Как у ванильных животных, но на любой земле (почвы TrailCraft — в #minecraft:dirt), а не только на траве. */
    public static boolean checkSpawnRules(EntityType<Capercaillie> type, LevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.DIRT) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON)) && isBrightEnoughToSpawn(level, pos);
    }
}

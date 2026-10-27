package io.github.ChanesCMD.trailcraft.animal;

import io.github.ChanesCMD.trailcraft.init.ModEntities;
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
 * Глухарь: ведёт себя как курица (бродит, паникует, машет крыльями и медленно падает, приманивается и разводится
 * семенами), но крупнее, ниже голосом и яиц не несёт. Лут — перья (loot_table/entities/capercaillie.json).
 */
public class Capercaillie extends Chicken {
    public Capercaillie(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.25);
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

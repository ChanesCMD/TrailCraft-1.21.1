package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.worldgen.CocoaPodFeature;
import io.github.ChanesCMD.trailcraft.worldgen.FallenBranchFeature;
import io.github.ChanesCMD.trailcraft.worldgen.LoosePebbleFeature;
import io.github.ChanesCMD.trailcraft.worldgen.NuggetDepositFeature;
import io.github.ChanesCMD.trailcraft.worldgen.PondFeature;
import io.github.ChanesCMD.trailcraft.worldgen.TrailTreeConfig;
import io.github.ChanesCMD.trailcraft.worldgen.TrailTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Фичеры генерации мира. Настройки частоты — в data/trailcraft/worldgen/placed_feature. */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TrailCraft.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LOOSE_PEBBLE =
            FEATURES.register("loose_pebble", LoosePebbleFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FALLEN_BRANCH =
            FEATURES.register("fallen_branch", FallenBranchFeature::new);

    /** Самородок на поверхности и большая жила его руды под ним. */
    public static final DeferredHolder<Feature<?>, Feature<NuggetDepositFeature.Config>> NUGGET_DEPOSIT =
            FEATURES.register("nugget_deposit", NuggetDepositFeature::new);

    /** Озерцо на равнине: 1–5 блоков воды вровень с землёй. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> POND =
            FEATURES.register("pond", PondFeature::new);

    /** Стручки какао на стволах пальм и эбеновых деревьев. */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> COCOA_PODS =
            FEATURES.register("cocoa_pods", CocoaPodFeature::new);

    public static final DeferredHolder<Feature<?>, Feature<TrailTreeConfig>> TRAIL_TREE =
            FEATURES.register("trail_tree", TrailTreeFeature::new);

    private ModFeatures() {}
}

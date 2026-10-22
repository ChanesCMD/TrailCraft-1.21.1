package io.github.ChanesCMD.trailcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Настройки дерева TrailCraft.
 *
 * @param wood      порода (id набора из ModWood, например "oak")
 * @param shape     форма кроны: round, cone, tall, pine, weeping, palm, acacia, sakura
 * @param oldChance доля старых деревьев (0 — всегда молодое, как из саженца; 1 — всегда старое)
 */
public record TrailTreeConfig(String wood, String shape, float oldChance) implements FeatureConfiguration {
    public static final Codec<TrailTreeConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("wood").forGetter(TrailTreeConfig::wood),
            Codec.STRING.fieldOf("shape").forGetter(TrailTreeConfig::shape),
            Codec.floatRange(0, 1).fieldOf("old_chance").forGetter(TrailTreeConfig::oldChance)
    ).apply(i, TrailTreeConfig::new));
}

package io.github.ChanesCMD.trailcraft.ore;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Породы, в которых лежат руды. Имя совпадает с id блока породы (trailcraft:gabbro и т. д.), кроме известняка — это
 * известняк Create (create:limestone) — и базальта (ванильный minecraft:basalt); их булыжник, гравий, плита и камешек —
 * свои (trailcraft:limestone_*, trailcraft:basalt_*).
 */
public enum Rock implements StringRepresentable {
    ANDESITE, BASALT, CHALK, DIORITE, DOLOMITE, GABBRO, GRANITE, GRAY_GRANITE, LIMESTONE, MARBLE, SANDSTONE, SHALE;

    private final String id = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Порода — блок другого мода или ванильный (свойства её булыжника и плиты задаются отдельно). */
    public boolean isForeign() {
        return !TrailCraft.MODID.equals(blockId().getNamespace());
    }

    /** Id блока самой породы. */
    public ResourceLocation blockId() {
        return switch (this) {
            case LIMESTONE -> ResourceLocation.fromNamespaceAndPath("create", id);
            case BASALT -> ResourceLocation.withDefaultNamespace(id);
            default -> ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, id);
        };
    }

    public Block block() {
        return BuiltInRegistries.BLOCK.get(blockId());
    }

    /** Порода по её блоку (null — не порода). */
    public static Rock of(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        for (Rock rock : values()) {
            if (rock.blockId().equals(id)) {
                return rock;
            }
        }
        return null;
    }
}

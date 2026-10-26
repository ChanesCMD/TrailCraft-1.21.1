package io.github.ChanesCMD.trailcraft.ore;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Породы, в которых лежат руды. Имя совпадает с id блока породы (trailcraft:limestone и т. д.). */
public enum Rock implements StringRepresentable {
    ANDESITE, BASALT, CHALK, DIORITE, DOLOMITE, GABBRO, GRANITE, GRAY_GRANITE, LIMESTONE, MARBLE, SANDSTONE, SHALE;

    private final String id = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return id;
    }
}

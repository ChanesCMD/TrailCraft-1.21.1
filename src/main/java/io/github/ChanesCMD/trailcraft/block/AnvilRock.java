package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.ore.Rock;
import net.minecraft.util.StringRepresentable;

/** Шесть твёрдых пород, из которых можно сделать каменную наковальню. */
public enum AnvilRock implements StringRepresentable {
    ANDESITE("andesite"), BASALT("basalt"), DIORITE("diorite"), GRANITE("granite"),
    GRAY_GRANITE("gray_granite"), GABBRO("gabbro");

    private final String id;

    AnvilRock(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Наковальня из этой породы или null, если порода мягкая. */
    public static AnvilRock of(Rock rock) {
        return switch (rock) {
            case ANDESITE -> ANDESITE;
            case BASALT -> BASALT;
            case DIORITE -> DIORITE;
            case GRANITE -> GRANITE;
            case GRAY_GRANITE -> GRAY_GRANITE;
            case GABBRO -> GABBRO;
            default -> null;
        };
    }
}

package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** Что можно вылепить из слепка (окно слепка). Новые формы — новой строкой. */
public enum ClayShape {
    JUG("jug", ModSmelting.UNFIRED_JUG::get);

    public final String id;
    private final Supplier<Item> result;

    ClayShape(String id, Supplier<Item> result) {
        this.id = id;
        this.result = result;
    }

    public Item result() {
        return result.get();
    }
}

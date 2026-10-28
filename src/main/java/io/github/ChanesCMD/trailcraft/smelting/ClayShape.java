package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** Что можно вылепить из слепка (окно слепка): сырой кувшин и сырые формы для литья. Новые — новой строкой. */
public enum ClayShape {
    JUG("jug", ModSmelting.UNFIRED_JUG::get),
    INGOT_MOLD(Mold.INGOT), PICKAXE_MOLD(Mold.PICKAXE), AXE_MOLD(Mold.AXE), SHOVEL_MOLD(Mold.SHOVEL), HOE_MOLD(Mold.HOE),
    KNIFE_MOLD(Mold.KNIFE), SPEAR_MOLD(Mold.SPEAR), SAW_MOLD(Mold.SAW), SICKLE_MOLD(Mold.SICKLE),
    SCYTHE_MOLD(Mold.SCYTHE), CUTTERS_MOLD(Mold.CUTTERS);

    public final String id;
    private final Supplier<Item> result;

    ClayShape(String id, Supplier<Item> result) {
        this.id = id;
        this.result = result;
    }

    /** Сырая форма для литья. */
    ClayShape(Mold mold) {
        this(mold.id + "_mold", () -> ModSmelting.UNFIRED_MOLDS.get(mold).get());
    }

    public Item result() {
        return result.get();
    }
}

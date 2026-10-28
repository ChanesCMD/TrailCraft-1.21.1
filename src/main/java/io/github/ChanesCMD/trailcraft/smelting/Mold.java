package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import net.minecraft.world.item.ItemStack;

/**
 * Формы для литья (как в TerraFirmaCraft): лепятся из слепка (ClayShape), обжигаются на костре. В форму слитка
 * льётся любой металл, в формы инструментов — только металлы, из которых есть инструменты (медь и бронзы); из формы
 * выходит слиток или головка инструмента (ModSmelting.HEADS), головку с черенком собирают на верстаке.
 * На одну форму — Smelting.UNIT металла.
 */
public enum Mold {
    INGOT("ingot"), PICKAXE("pickaxe"), AXE("axe"), SHOVEL("shovel"), HOE("hoe"), KNIFE("knife"), SPEAR("spear"),
    SAW("saw"), SICKLE("sickle"), SCYTHE("scythe"), CUTTERS("cutters");

    public final String id;

    Mold(String id) {
        this.id = id;
    }

    /** Что выйдет из формы с этим металлом (пустой стак — этот металл в форму не льётся). */
    public ItemStack result(Metal metal) {
        if (this == INGOT) {
            return new ItemStack(metal.ingot());
        }
        String material = metal.toolMaterial();
        var head = material == null ? null : ModSmelting.HEADS.get(material + "_" + id);
        return head == null ? ItemStack.EMPTY : new ItemStack(head.get());
    }
}

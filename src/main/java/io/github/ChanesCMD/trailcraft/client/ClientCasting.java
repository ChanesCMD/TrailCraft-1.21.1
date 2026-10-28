package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.smelting.JugItem;
import io.github.ChanesCMD.trailcraft.smelting.MoldItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Вид кувшина и форм: свойство trailcraft:molten — в кувшине жидкий металл, trailcraft:filled — форма залита
 * (overrides в models/item/jug.json и models/item/&lt;форма&gt;_mold.json, пишет scripts/gen_casting.py).
 */
public final class ClientCasting {
    public static final ResourceLocation MOLTEN = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "molten");
    public static final ResourceLocation FILLED = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "filled");

    public static void registerItemProperties() {
        ItemProperties.register(ModSmelting.JUG.get(), MOLTEN,
                (stack, level, entity, seed) -> JugItem.molten(stack) != null ? 1 : 0);
        ModSmelting.MOLDS.values().forEach(mold -> ItemProperties.register(mold.get(), FILLED,
                (stack, level, entity, seed) -> MoldItem.cast(stack) != null ? 1 : 0));
    }

    private ClientCasting() {}
}

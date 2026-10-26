package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModOres;
import io.github.ChanesCMD.trailcraft.item.OreBlockItem;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Предмет руды рисуется в своей породе: свойство trailcraft:rock = номер породы в списке руды × 0,1,
 * модель предмета выбирает вариант по нему (overrides в models/item/&lt;руда&gt;_ore.json).
 */
public final class ClientOres {
    public static final ResourceLocation ROCK = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "rock");

    public static void registerItemProperties() {
        for (Ore ore : Ore.values()) {
            ItemProperties.register(ModOres.BLOCK_ITEMS.get(ore).get(), ROCK,
                    (stack, level, entity, seed) -> ore.rocks.indexOf(OreBlockItem.rock(stack)) * 0.1f);
        }
    }

    private ClientOres() {}
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.smelting.JugItem;
import io.github.ChanesCMD.trailcraft.smelting.MoldItem;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Вид кувшина и форм: свойство trailcraft:molten — в кувшине жидкий металл, trailcraft:filled — форма залита
 * (overrides в models/item/jug.json и models/item/&lt;форма&gt;_mold.json, пишет scripts/gen_casting.py); второй слой
 * залитой формы (металл) красится в цвет залитого металла (Metal.color).
 */
@EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientCasting {
    public static final ResourceLocation MOLTEN = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "molten");
    public static final ResourceLocation FILLED = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "filled");

    public static void registerItemProperties() {
        ItemProperties.register(ModSmelting.JUG.get(), MOLTEN,
                (stack, level, entity, seed) -> JugItem.molten(stack) != null ? 1 : 0);
        ModSmelting.MOLDS.values().forEach(mold -> ItemProperties.register(mold.get(), FILLED,
                (stack, level, entity, seed) -> MoldItem.cast(stack) != null ? 1 : 0));
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            Metal metal = MoldItem.cast(stack);
            return tintIndex == 1 && metal != null ? 0xFF000000 | metal.color : -1;
        }, ModSmelting.MOLDS.values().stream().map(DeferredItem::get).toArray(Item[]::new));
    }

    private ClientCasting() {}
}

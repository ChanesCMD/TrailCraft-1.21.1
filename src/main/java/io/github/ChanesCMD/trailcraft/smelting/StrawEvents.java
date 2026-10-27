package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Солома: трава (#trailcraft:straw_grass), срезанная ножом, серпом или косой (#trailcraft:straw_tools), даёт 1 солому. */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class StrawEvents {
    public static final TagKey<Block> STRAW_GRASS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "straw_grass"));
    public static final TagKey<Item> STRAW_TOOLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "straw_tools"));

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!event.getState().is(STRAW_GRASS) || !event.getTool().is(STRAW_TOOLS)) {
            return;
        }
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                new ItemStack(ModSmelting.STRAW.get())));
    }

    private StrawEvents() {}
}

package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ChanesCMD.trailcraft.block.entity.WoodChestBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModChests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.HashMap;
import java.util.Map;

/** Сундук своей породы в руке и в инвентаре — та же модель, что на земле (модель предмета — minecraft:item/chest). */
public class WoodChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final Map<net.minecraft.world.level.block.Block, WoodChestBlockEntity> chests = new HashMap<>();

    public WoodChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        if (stack.getItem() instanceof BlockItem item) {
            WoodChestBlockEntity chest = chests.computeIfAbsent(item.getBlock(),
                    block -> new WoodChestBlockEntity(BlockPos.ZERO, block.defaultBlockState()));
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chest, pose, buffers, light, overlay);
        }
    }

    public static void register(RegisterClientExtensionsEvent event) {
        WoodChestItemRenderer renderer = new WoodChestItemRenderer();
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        ModChests.ITEMS.values().forEach(item -> event.registerItem(extensions, item.get()));
    }
}

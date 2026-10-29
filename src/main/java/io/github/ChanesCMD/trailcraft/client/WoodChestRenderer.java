package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.WoodChestBlock;
import io.github.ChanesCMD.trailcraft.block.entity.WoodChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Сундук своей породы: текстура entity/chest/<порода>, у двойного — _left / _right (атлас сундуков). */
public class WoodChestRenderer extends ChestRenderer<WoodChestBlockEntity> {
    public WoodChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Material getMaterial(WoodChestBlockEntity chest, ChestType type) {
        String wood = chest.getBlockState().getBlock() instanceof WoodChestBlock block ? block.wood : "oak";
        String suffix = switch (type) {
            case LEFT -> "_left";
            case RIGHT -> "_right";
            default -> "";
        };
        return new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "entity/chest/" + wood + suffix));
    }
}

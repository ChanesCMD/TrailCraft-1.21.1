package io.github.ChanesCMD.trailcraft.client.model;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModReinforced;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Паровой двигатель и свисток Create на укреплённом баке (init/ModReinforced) — в тёмной гамме прочного листа
 * (ReinforcedSprites). Части, которые рисует рендер Create (поршень, шатун), остаются медными.
 */
public class ReinforcedAttachmentModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> ON_REINFORCED = new ModelProperty<>();

    public ReinforcedAttachmentModel(BakedModel original) {
        super(original);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                           @NotNull ModelData modelData) {
        Direction dir = state.getBlock() instanceof SteamEngineBlock ? SteamEngineBlock.getConnectedDirection(state)
                : state.getBlock() instanceof WhistleBlock ? WhistleBlock.getAttachedDirection(state) : null;
        boolean reinforced = dir != null && (isReinforcedTank(level, pos.relative(dir)) || isReinforcedTank(level, pos.relative(dir.getOpposite())));
        return super.getModelData(level, pos, state, modelData).derive().with(ON_REINFORCED, reinforced).build();
    }

    private static boolean isReinforcedTank(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(ModReinforced.TANK.get());
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (!Boolean.TRUE.equals(data.get(ON_REINFORCED))) {
            return quads;
        }
        return ReinforcedSprites.swap(quads);
    }

}

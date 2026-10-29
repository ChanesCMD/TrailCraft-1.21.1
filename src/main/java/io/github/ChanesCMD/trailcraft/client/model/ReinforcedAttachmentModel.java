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
 * Паровой двигатель и свисток Create на укреплённом баке (init/ModReinforced) — в тёмной гамме прочного листа: медные
 * текстуры модели (create:block/X) подменяются нашими trailcraft:block/reinforced/X (их пишет
 * scripts/gen_reinforced_parts.py). Части, которые рисует рендер Create (поршень, шатун), остаются медными.
 */
public class ReinforcedAttachmentModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> ON_REINFORCED = new ModelProperty<>();
    /** Спрайт Create → наш (или сам же, если нашего нет). */
    private static final Map<TextureAtlasSprite, TextureAtlasSprite> SPRITES = new HashMap<>();

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
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite from = quad.getSprite();
            TextureAtlasSprite to = reinforced(from);
            out.add(to == from ? quad : retexture(quad, from, to));
        }
        return out;
    }

    private static TextureAtlasSprite reinforced(TextureAtlasSprite sprite) {
        return SPRITES.computeIfAbsent(sprite, s -> {
            ResourceLocation name = s.contents().name();
            if (!"create".equals(name.getNamespace()) || !name.getPath().startsWith("block/")) {
                return s;
            }
            ResourceLocation ours = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID,
                    "block/reinforced/" + name.getPath().substring("block/".length()));
            TextureAtlasSprite found = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(ours);
            return found.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? s : found;
        });
    }

    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to) {
        int[] v = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            int at = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float u = Float.intBitsToFloat(v[at]), w = Float.intBitsToFloat(v[at + 1]);
            v[at] = Float.floatToRawIntBits(to.getU(from.getUOffset(u)));
            v[at + 1] = Float.floatToRawIntBits(to.getV(from.getVOffset(w)));
        }
        return new BakedQuad(v, quad.getTintIndex(), quad.getDirection(), to, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** Атлас перестроен (смена ресурспака) — спрайты другие. */
    public static void clearCache() {
        SPRITES.clear();
    }
}

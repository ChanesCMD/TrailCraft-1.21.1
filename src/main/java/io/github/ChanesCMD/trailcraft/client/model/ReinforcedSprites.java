package io.github.ChanesCMD.trailcraft.client.model;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Перекраска моделей Create в тёмную гамму прочного листа на лету: в квадах спрайт create:block/X подменяется нашим
 * trailcraft:block/reinforced/X (их пишет scripts/gen_reinforced_parts.py), если такой есть. Нужна для того, что Create
 * дорисовывает своими моделями: ободки, стыки, сливы и корпус укреплённых труб (PipeAttachmentModel), двигатель и свисток
 * на укреплённом баке (ReinforcedAttachmentModel).
 */
public final class ReinforcedSprites {
    /** Спрайт Create → наш (или сам же, если нашего нет). */
    private static final Map<TextureAtlasSprite, TextureAtlasSprite> SPRITES = new HashMap<>();

    private ReinforcedSprites() {}

    public static List<BakedQuad> swap(List<BakedQuad> quads) {
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite from = quad.getSprite();
            TextureAtlasSprite to = reinforced(from);
            if (to != from && out == null) {
                out = new ArrayList<>(quads.subList(0, i));
            }
            if (out != null) {
                out.add(to == from ? quad : retexture(quad, from, to));
            }
        }
        return out == null ? quads : out;
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

    /** Модель, у которой все спрайты Create подменены нашими. */
    public static class Model extends BakedModelWrapper<BakedModel> {
        public Model(BakedModel original) {
            super(original);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                 @NotNull ModelData data, @Nullable RenderType renderType) {
            return swap(super.getQuads(state, side, rand, data, renderType));
        }
    }
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цвета сезонов на клиенте (season/Seasons): листва лиственных пород осенью жёлтая, зимой белая — в квадах её модели
 * спрайт trailcraft:block/X подменяется trailcraft:block/season/&lt;сезон&gt;/X (пишет scripts/gen_season_textures.py;
 * модели оборачивает ClientEvents.wrapPipeModels); трава весной зеленее, осенью желтее, зимой в инее, а под снегом
 * белая — {@link #grass}. Сменился сезон — чанки перестраиваются.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class SeasonColors {
    private static final int SPRING_GRASS = 0x4FC23A, AUTUMN_GRASS = 0xC9B24A, WINTER_GRASS = 0xEEF2F6;
    /** Доля сезонного цвета в цвете травы: весной, осенью, зимой (в инее и под снегом). */
    private static final float SPRING_MIX = 0.25f, AUTUMN_MIX = 0.4f, FROST_MIX = 0.55f, SNOWED_MIX = 0.9f;

    /** Сезон, в котором нарисованы чанки. */
    private static volatile Calendar.Season shown = Calendar.Season.SUMMER;
    private static final Map<Calendar.Season, Map<TextureAtlasSprite, TextureAtlasSprite>> SPRITES =
            new EnumMap<>(Calendar.Season.class);

    static {
        for (Calendar.Season season : Calendar.Season.values()) {
            SPRITES.put(season, new ConcurrentHashMap<>());
        }
    }

    private SeasonColors() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Seasons.update(mc.level.getDayTime());
        Calendar.Season season = Seasons.current();
        if (season != shown) {
            shown = season;
            mc.levelRenderer.allChanged();
        }
    }

    /** Цвет травы с поправкой на сезон; pos == null — предмет или без мира. */
    public static int grass(int rgb, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        return switch (shown) {
            case SPRING -> mix(rgb, SPRING_GRASS, SPRING_MIX);
            case SUMMER -> rgb;
            case AUTUMN -> mix(rgb, AUTUMN_GRASS, AUTUMN_MIX);
            case WINTER -> mix(rgb, WINTER_GRASS, level != null && pos != null
                    && level.getBlockState(pos.above()).is(Blocks.SNOW) ? SNOWED_MIX : FROST_MIX);
        };
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static TextureAtlasSprite seasonal(TextureAtlasSprite sprite, Calendar.Season season) {
        return SPRITES.get(season).computeIfAbsent(sprite, s -> {
            ResourceLocation name = s.contents().name();
            if (!TrailCraft.MODID.equals(name.getNamespace()) || !name.getPath().startsWith("block/")) {
                return s;
            }
            ResourceLocation ours = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "block/season/"
                    + season.name().toLowerCase() + "/" + name.getPath().substring("block/".length()));
            TextureAtlasSprite found = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(ours);
            return found.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? s : found;
        });
    }

    private static List<BakedQuad> swap(List<BakedQuad> quads, Calendar.Season season) {
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite from = quad.getSprite();
            TextureAtlasSprite to = seasonal(from, season);
            if (to != from && out == null) {
                out = new ArrayList<>(quads.subList(0, i));
            }
            if (out != null) {
                out.add(to == from ? quad : retexture(quad, from, to));
            }
        }
        return out == null ? quads : out;
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
        SPRITES.values().forEach(Map::clear);
    }

    /** Модель листвы: осенью и зимой — сезонные текстуры (в мире; предмет в руке — летний). */
    public static class LeavesModel extends BakedModelWrapper<BakedModel> {
        public LeavesModel(BakedModel original) {
            super(original);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                 @NotNull ModelData data, @Nullable RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            Calendar.Season season = shown;
            return state == null || season == Calendar.Season.SPRING || season == Calendar.Season.SUMMER
                    ? quads : swap(quads, season);
        }
    }
}

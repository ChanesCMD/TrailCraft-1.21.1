package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.calendar.Calendar;
import io.github.ChanesCMD.trailcraft.season.Seasons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цвета сезонов на клиенте (season/Seasons): листва лиственных пород осенью жёлтая, зимой белая — в квадах её модели
 * спрайт trailcraft:block/X подменяется trailcraft:block/season/&lt;сезон&gt;/X (пишет scripts/gen_season_textures.py;
 * модели оборачивает ClientEvents.wrapPipeModels); трава весной зеленее, осенью желтее, зимой в инее, а под снегом
 * белая — {@link #grass}. В тёплых биомах (Seasons.isWarm) весной и осенью как летом, зимой листва (block/season/warm_winter/X)
 * и трава чуть желтеют. Сменился сезон — чанки перестраиваются.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class SeasonColors {
    private static final int SPRING_GRASS = 0x4FC23A, AUTUMN_GRASS = 0xC9B24A, WINTER_GRASS = 0xEEF2F6;
    /** Доля сезонного цвета в цвете травы: весной, осенью, зимой (в инее и под снегом). */
    private static final float SPRING_MIX = 0.25f, AUTUMN_MIX = 0.4f, FROST_MIX = 0.55f, SNOWED_MIX = 0.9f;
    /** Тёплые биомы зимой: доля осеннего цвета в траве. */
    private static final float WARM_WINTER_MIX = 0.12f;
    /** Листва в тёплом биоме — кладётся в ModelData при сборке чанка (LeavesModel.getModelData). */
    private static final ModelProperty<Boolean> WARM = new ModelProperty<>();

    /** Сезон, в котором нарисованы чанки. */
    private static volatile Calendar.Season shown = Calendar.Season.SUMMER;
    /** Папка сезонных текстур (autumn, winter, warm_winter) → спрайт листвы → сезонный. */
    private static final Map<String, Map<TextureAtlasSprite, TextureAtlasSprite>> SPRITES = new ConcurrentHashMap<>();

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
        if (isWarm(pos)) {
            return shown == Calendar.Season.WINTER ? mix(rgb, AUTUMN_GRASS, WARM_WINTER_MIX) : rgb;
        }
        return switch (shown) {
            case SPRING -> mix(rgb, SPRING_GRASS, SPRING_MIX);
            case SUMMER -> rgb;
            case AUTUMN -> mix(rgb, AUTUMN_GRASS, AUTUMN_MIX);
            case WINTER -> mix(rgb, WINTER_GRASS, level != null && pos != null
                    && level.getBlockState(pos.above()).is(Blocks.SNOW) ? SNOWED_MIX : FROST_MIX);
        };
    }

    /** Тёплый ли биом в этом месте (биом берётся из мира клиента, как у BiomeColors при сборке чанка). */
    private static boolean isWarm(@Nullable BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        return pos != null && level != null && Seasons.isWarm(level.getBiome(pos).value());
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static TextureAtlasSprite seasonal(TextureAtlasSprite sprite, String folder) {
        return SPRITES.computeIfAbsent(folder, f -> new ConcurrentHashMap<>()).computeIfAbsent(sprite, s -> {
            ResourceLocation name = s.contents().name();
            if (!TrailCraft.MODID.equals(name.getNamespace()) || !name.getPath().startsWith("block/")) {
                return s;
            }
            ResourceLocation ours = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "block/season/"
                    + folder + "/" + name.getPath().substring("block/".length()));
            TextureAtlasSprite found = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(ours);
            return found.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? s : found;
        });
    }

    private static List<BakedQuad> swap(List<BakedQuad> quads, String folder) {
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite from = quad.getSprite();
            TextureAtlasSprite to = seasonal(from, folder);
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

    /** Модель листвы: осенью и зимой — сезонные текстуры (в мире; предмет в руке — летний), в тёплых биомах — только зимой. */
    public static class LeavesModel extends BakedModelWrapper<BakedModel> {
        public LeavesModel(BakedModel original) {
            super(original);
        }

        @Override
        public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                               @NotNull ModelData data) {
            return super.getModelData(level, pos, state, data).derive().with(WARM, isWarm(pos)).build();
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                 @NotNull ModelData data, @Nullable RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            if (state == null) {
                return quads;
            }
            boolean warm = Boolean.TRUE.equals(data.get(WARM));
            return switch (shown) {
                case SPRING, SUMMER -> quads;
                case AUTUMN -> warm ? quads : swap(quads, "autumn");
                case WINTER -> swap(quads, warm ? "warm_winter" : "winter");
            };
        }
    }
}

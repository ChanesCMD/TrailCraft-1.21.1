package io.github.ChanesCMD.trailcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.OreBlock;
import io.github.ChanesCMD.trailcraft.block.PebbleBlock;
import io.github.ChanesCMD.trailcraft.init.ModOres;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import io.github.ChanesCMD.trailcraft.ore.Ore;
import io.github.ChanesCMD.trailcraft.ore.Rock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Самородок на поверхности, а под ним, в верхних слоях, большая жила его руды (VEIN_MIN..VEIN_MAX блоков).
 * Жила заменяет любой твёрдый природный блок; порода руды — та, что заменена, если она подходит руде, иначе первая
 * порода руды. Фича вызывается в каждом чанке, но ставится ровно в одном чанке каждой клетки CELL×CELL чанков
 * (4 сектора 9×9): чанк выбирается по сиду мира, клетке и руде, не ближе MARGIN к краю клетки — так места
 * распределены равномерно, а не кучками, как при случайном шансе.
 */
public class NuggetDepositFeature extends Feature<NuggetDepositFeature.Config> {
    private static final int VEIN_MIN = 15, VEIN_MAX = 25;
    /** Центр жилы — на столько блоков ниже поверхности. */
    private static final int DEPTH_MIN = 3, DEPTH_MAX = 8;
    private static final int RADIUS = 3;
    private static final int CELL = 18, MARGIN = 3;

    /** @param ore id руды из {@link Ore} (например, malachite) */
    public record Config(String ore) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("ore").forGetter(Config::ore)).apply(i, Config::new));
    }

    public NuggetDepositFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Ore ore = ore(ctx.config().ore());
        if (ore == null || !ModOres.NUGGET_BLOCKS.containsKey(ore)) {
            return false;
        }
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos pos = ctx.origin();
        if (!isChosenChunk(level.getSeed(), new ChunkPos(pos), ore)) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        boolean water = here.is(Blocks.WATER) && here.getFluidState().isSource();
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!here.isAir() && !water || !ground.isFaceSturdy(level, below, Direction.UP)
                || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS) || ground.is(ModTags.THIN_LOG_BLOCKS)) {
            return false;
        }
        BlockState nugget = ModOres.NUGGET_BLOCKS.get(ore).get().defaultBlockState().setValue(PebbleBlock.WATERLOGGED, water);
        level.setBlock(pos, nugget, Block.UPDATE_CLIENTS);
        placeVein(level, random, ore, below.below(DEPTH_MIN + random.nextInt(DEPTH_MAX - DEPTH_MIN + 1)));
        return true;
    }

    /** Этот ли чанк выбран для руды в своей клетке CELL×CELL чанков. */
    private static boolean isChosenChunk(long seed, ChunkPos chunk, Ore ore) {
        int cellX = Math.floorDiv(chunk.x, CELL), cellZ = Math.floorDiv(chunk.z, CELL);
        long h = mix(seed ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ (ore.ordinal() + 1) * 0x165667B19E3779F9L);
        int span = CELL - 2 * MARGIN;
        int x = MARGIN + (int) Math.floorMod(h, (long) span), z = MARGIN + (int) Math.floorMod(h >>> 32, (long) span);
        return chunk.x == cellX * CELL + x && chunk.z == cellZ * CELL + z;
    }

    /** Перемешивание битов (splitmix64). */
    private static long mix(long h) {
        h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
        return h ^ h >>> 31;
    }

    /** Жила — ближайшие к центру клетки сплюснутого шара (со случайным разбросом), пока не наберётся размер. */
    private static void placeVein(WorldGenLevel level, RandomSource random, Ore ore, BlockPos center) {
        OreBlock block = ModOres.BLOCKS.get(ore).get();
        List<BlockPos> cells = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    cells.add(center.offset(dx, dy, dz));
                    weights.add(dx * dx + 2 * dy * dy + dz * dz + random.nextFloat() * 4);
                }
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Float.compare(weights.get(a), weights.get(b)));
        int size = VEIN_MIN + random.nextInt(VEIN_MAX - VEIN_MIN + 1), placed = 0;
        for (int i = 0; i < order.size() && placed < size; i++) {
            BlockPos p = cells.get(order.get(i));
            BlockState state = level.getBlockState(p);
            if (!replaceable(level, p, state)) {
                continue;
            }
            level.setBlock(p, block.defaultBlockState().setValue(ore.rockProperty, rockFor(ore, state)), Block.UPDATE_CLIENTS);
            placed++;
        }
    }

    /** Твёрдый природный блок: не воздух, не жидкость, не блок с содержимым, не бедрок. */
    private static boolean replaceable(WorldGenLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0 && state.isSolidRender(level, pos)
                && !(state.getBlock() instanceof OreBlock);
    }

    /** Порода руды: заменённый блок, если это одна из её пород, иначе первая. */
    private static Rock rockFor(Ore ore, BlockState replaced) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(replaced.getBlock());
        if (TrailCraft.MODID.equals(id.getNamespace())) {
            for (Rock rock : ore.rocks) {
                if (rock.getSerializedName().equals(id.getPath())) {
                    return rock;
                }
            }
        }
        return ore.rocks.get(0);
    }

    private static Ore ore(String id) {
        for (Ore ore : Ore.values()) {
            if (ore.id.equals(id)) {
                return ore;
            }
        }
        return null;
    }
}

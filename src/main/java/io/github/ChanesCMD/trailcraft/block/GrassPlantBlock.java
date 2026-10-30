package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Простое растение-трава: крестообразная модель, без коллизии, растёт на дёрне и обычной земле.
 * Снежная трава (snowable): снегопад кладёт в её клетку слои снега (свойство SNOW, 0 — снега нет, ServerLevelMixin),
 * трава торчит из снега и зимой белая (client/SeasonColors.grass); снег тает по слою, как обычный (randomTick),
 * метла его сметает (BroomItem).
 */
public class GrassPlantBlock extends BushBlock {
    public static final MapCodec<GrassPlantBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("height").forGetter(b -> b.height),
            Codec.BOOL.optionalFieldOf("snowable", false).forGetter(b -> b.snowable),
            propertiesCodec()
    ).apply(instance, (height, snowable, props) -> new GrassPlantBlock(props, height, snowable)));

    /** Слоёв снега в клетке с травой (как LAYERS у снега): 0 — снега нет. См. SnowCover. */
    public static final IntegerProperty SNOW = SnowCover.SNOW;

    private final int height;
    private final boolean snowable;
    private final VoxelShape shape;

    public GrassPlantBlock(BlockBehaviour.Properties properties, int height) {
        this(properties, height, true);
    }

    public GrassPlantBlock(BlockBehaviour.Properties properties, int height, boolean snowable) {
        super(properties);
        this.height = height;
        this.snowable = snowable;
        this.shape = Block.box(2.0, 0.0, 2.0, 14.0, height, 14.0);
        registerDefaultState(stateDefinition.any().setValue(SNOW, 0));
    }

    /** Может ли снегопад класть снег в клетку с этой травой. */
    public boolean isSnowable() {
        return snowable;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNOW);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(SNOW) > 0;
    }

    /** Снег в клетке с травой тает так же, как слой снега (SnowCover.melt). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SnowCover.melt(state, level, pos, random);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(ModTags.GRASS_PLANTABLE);
    }
}

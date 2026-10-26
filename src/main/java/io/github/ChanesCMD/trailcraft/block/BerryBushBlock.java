package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.farm.Farm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.function.Supplier;

/**
 * Дикий ягодный куст (смородина, морошка, крыжовник, ежевика). Растёт как сладкие ягоды: стадии 0–3,
 * на 2-й появляются ягоды, на 3-й их больше. ПКМ по кусту с ягодами — ягоды падают, куст остаётся (стадия 1).
 * Сломанный куст даёт ветки (лут-таблица). Растёт на почве и дёрне. Числа — в farm/Farm.
 */
public class BerryBushBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<BerryBushBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("berry").forGetter(b -> b.berry.get()),
            Codec.INT.fieldOf("height").forGetter(b -> b.height),
            propertiesCodec()
    ).apply(instance, (berry, height, props) -> new BerryBushBlock(props, () -> berry, height)));
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;
    /** С этой стадии на кусте есть ягоды. */
    public static final int BERRY_AGE = 2;
    private static final VoxelShape SPROUT_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);

    private final Supplier<? extends Item> berry;
    private final int height;
    private final VoxelShape shape;

    /** @param height высота взрослого куста в пикселях (морошка низкая) */
    public BerryBushBlock(Properties properties, Supplier<? extends Item> berry, int height) {
        super(properties);
        this.berry = berry;
        this.height = height;
        this.shape = Block.box(1.0, 0.0, 1.0, 15.0, height, 15.0);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SPROUT_SHAPE : shape;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && level.getRawBrightness(pos.above(), 0) >= 9
                && CommonHooks.canCropGrow(level, pos, state, random.nextInt(Farm.BUSH_GROWTH_CHANCE) == 0)) {
            BlockState grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(grown));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // Костная мука по неспелому кусту удобряет его, а не собирает ягоды
        if (state.getValue(AGE) < MAX_AGE && stack.is(Items.BONE_MEAL)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age < BERRY_AGE) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        int count = Farm.BERRIES_MIN + level.random.nextInt(Farm.BERRIES_MAX - Farm.BERRIES_MIN + 1) + (age == MAX_AGE ? 1 : 0);
        popResource(level, pos, new ItemStack(berry.get(), count));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        BlockState picked = state.setValue(AGE, 1);
        level.setBlock(pos, picked, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
    }
}

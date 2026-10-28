package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import io.github.ChanesCMD.trailcraft.block.entity.TrailCampfireBlockEntity;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Костёр TrailCraft: слой золы и 0–4 тонких бревна (LOGS). Складывается огнивом (FirestarterItem) из брошенных
 * на землю тонких брёвен — сколько бросили, столько и лежит, но не больше MAX_LOGS — и 4 веток; снизу сразу зола.
 * Брёвна: два нижних вдоль, два верхних поперёк (третье — дальнее от того, кто сложил); в огне их текстура обуглена.
 * Горящие брёвна выгорают по одному сверху (TrailCampfireBlockEntity), без брёвен огонь гаснет, а зола остаётся слоем:
 * на неё, как и в горящий костёр, ПКМ кладут тонкие брёвна, потом разжигают огнивом. Лопата и кремнёвая мотыга
 * (#minecraft:shovels) тушат горящий костёр, а с потухшего без брёвен собирают золу.
 * ПКМ тем, что жарится на костре (campfire_cooking), — в один из 4 слотов; слоты лежат на брёвнах и опускаются вместе
 * с ними до золы (slotPosition), под занятым слотом — глиняная тарелочка (client/TrailCampfireRenderer).
 */
public class TrailCampfireBlock extends BaseEntityBlock {
    public static final MapCodec<TrailCampfireBlock> CODEC = simpleCodec(TrailCampfireBlock::new);
    public static final int MAX_LOGS = 4;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty LOGS = IntegerProperty.create("logs", 0, MAX_LOGS);
    /** Только зола; одно-два нижних бревна; с верхними брёвнами. */
    private static final VoxelShape ASH_SHAPE = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape LOW_SHAPE = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape HIGH_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    /**
     * Слоты еды в модели, смотрящей на юг (пиксели x, z): над пересечениями брёвен. Нижние брёвна лежат вдоль z
     * на x 1–5 и 11–15 (высота 1–5), верхние — вдоль x на z 11–15 (третье бревно) и 1–5 (четвёртое), высота 4–8.
     */
    private static final int[][] SLOTS = {{3, 13}, {13, 13}, {13, 3}, {3, 3}};

    public TrailCampfireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(LOGS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, LOGS);
    }

    /** Предмет костра (креатив, JEI) ставит потухший костёр из четырёх брёвен. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(LOGS, MAX_LOGS);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int logs = state.getValue(LOGS);
        return logs == 0 ? ASH_SHAPE : logs <= 2 ? LOW_SHAPE : HIGH_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrailCampfireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModBlockEntities.CAMPFIRE.get(), TrailCampfireBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.CAMPFIRE.get(), TrailCampfireBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrailCampfireBlockEntity campfire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int logs = state.getValue(LOGS);
        // Тонкое бревно — подложить (пока есть место; на полный костёр оно ляжет жариться в уголь)
        if (stack.is(ModTags.THIN_LOGS) && logs < MAX_LOGS) {
            if (!level.isClientSide) {
                campfire.addLog(level, pos, state, stack);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(ItemTags.SHOVELS) && (state.getValue(LIT) || logs == 0)) {
            if (!level.isClientSide) {
                if (state.getValue(LIT)) {
                    douse(level, pos, state);
                } else {
                    popResource(level, pos, new ItemStack(ModItems.ASH.get(), campfire.ash()));
                    level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
                    level.removeBlock(pos, false);
                }
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Optional<RecipeHolder<CampfireCookingRecipe>> recipe = campfire.getCookableRecipe(stack);
        if (recipe.isPresent()) {
            if (!level.isClientSide && campfire.placeFood(player, stack, recipe.get().value().getCookingTime())) {
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Сломан лопатой — заодно собрана и зола. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                              ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (tool.is(ItemTags.SHOVELS) && blockEntity instanceof TrailCampfireBlockEntity campfire) {
            popResource(level, pos, new ItemStack(ModItems.ASH.get(), campfire.ash()));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TrailCampfireBlockEntity campfire) {
            campfire.dropContents(level, pos, state.getValue(LOGS), state.getValue(LIT));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(LIT) && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().campfire(), 1.0f);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.3, z = pos.getZ() + 0.5;
        if (random.nextInt(10) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f + random.nextFloat(),
                    random.nextFloat() * 0.7f + 0.6f, false);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y, z, random.nextFloat() / 2.0f, 5.0E-5, random.nextFloat() / 2.0f);
        }
    }

    /**
     * Складывает и сразу разжигает костёр из брёвен (по одному, снизу вверх). Если на месте уже лежит зола костра,
     * брёвна ложатся на неё (слоты с едой и зола остаются), иначе ставится новый костёр, повёрнутый по facing.
     */
    public static void build(Level level, BlockPos pos, Direction facing, List<ItemStack> logs) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.CAMPFIRE.get())) {
            state = ModBlocks.CAMPFIRE.get().defaultBlockState().setValue(FACING, facing);
            level.setBlockAndUpdate(pos, state);
        }
        for (ItemStack log : logs) {
            if (level.getBlockEntity(pos) instanceof TrailCampfireBlockEntity campfire) {
                campfire.addLog(level, pos, level.getBlockState(pos), log);
            }
        }
        light(level, pos);
    }

    /** Разжигает потухший костёр, если в нём есть брёвна; false — гореть нечему или уже горит. */
    public static boolean light(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.CAMPFIRE.get()) || state.getValue(LIT) || state.getValue(LOGS) == 0) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof TrailCampfireBlockEntity campfire) {
            campfire.ignite(state.getValue(LOGS));
        }
        level.setBlockAndUpdate(pos, state.setValue(LIT, true));
        return true;
    }

    /** Тушит костёр: брёвна остаются (обугленными), их можно снова разжечь. */
    public static void douse(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(LIT, false));
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /** Поверхность (в пикселях) под слотом в модели, смотрящей на юг: верхнее бревно, нижнее или зола. */
    private static int surface(int slot, int logs) {
        int x = SLOTS[slot][0], z = SLOTS[slot][1];
        if (z == 13 && logs >= 3 || z == 3 && logs >= 4) {
            return 8;
        }
        if (x == 3 && logs >= 1 || x == 13 && logs >= 2) {
            return 5;
        }
        return 1;
    }

    /** Где лежит еда слота (в долях блока от его угла): над своим бревном или на золе, с учётом поворота костра. */
    public static Vec3 slotPosition(BlockState state, int slot) {
        double dx = SLOTS[slot][0] / 16.0 - 0.5, dz = SLOTS[slot][1] / 16.0 - 0.5;
        // поворот как у модели в блокстейте: y = facing.toYRot(), то есть по 90° столько раз, сколько get2DDataValue
        for (int k = 0; k < state.getValue(FACING).get2DDataValue(); k++) {
            double t = dx;
            dx = -dz;
            dz = t;
        }
        return new Vec3(0.5 + dx, surface(slot, state.getValue(LOGS)) / 16.0, 0.5 + dz);
    }
}

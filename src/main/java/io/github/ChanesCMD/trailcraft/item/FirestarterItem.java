package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.block.TrailCampfireBlock;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Огниво (как firestarter в TFC): зажать ПКМ, наведясь на место, где лежат выброшенные тонкие брёвна и 4 ветки, —
 * через 2 секунды ветки сгорают, а брёвна (сколько бросили, но не больше TrailCampfireBlock.MAX_LOGS) складываются
 * в горящий костёр на слое золы. Так же складывает костёр на золе старого и зажигает потухший костёр с брёвнами
 * (и ванильный).
 */
public class FirestarterItem extends Item {
    public static final int DURABILITY = 24;
    /** Сколько тиков держать ПКМ. */
    public static final int USE_TICKS = 40;
    /** Что должно лежать на месте костра: хотя бы столько тонких брёвен (берётся до MAX_LOGS) и столько веток. */
    public static final int LOGS = 1, BRANCHES = 4;

    public FirestarterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            entity.stopUsingItem();
            return;
        }
        // Дымок там, куда смотрит игрок, — чем дольше трёт, тем гуще
        if (level.isClientSide && remainingUseDuration % 4 == 0) {
            Vec3 at = hit.getLocation();
            level.addParticle(remainingUseDuration < USE_TICKS / 2 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE,
                    at.x, at.y, at.z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) return stack;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK && ignite(level, player, hit)) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
        return stack;
    }

    /** Зажигает потухший костёр или складывает новый (или на золе старого) из лежащих брёвен и веток. */
    private static boolean ignite(Level level, Player player, BlockHitResult hit) {
        BlockPos clicked = hit.getBlockPos();
        BlockState clickedState = level.getBlockState(clicked);
        boolean onAsh = clickedState.is(ModBlocks.CAMPFIRE.get());
        if (onAsh && TrailCampfireBlock.light(level, clicked)) {
            level.playSound(null, clicked, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }
        if (onAsh && clickedState.getValue(TrailCampfireBlock.LOGS) > 0) {
            return false; // уже горит
        }
        if (CampfireBlock.canLight(clickedState)) {
            level.setBlockAndUpdate(clicked, clickedState.setValue(BlockStateProperties.LIT, true));
            level.playSound(null, clicked, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }

        BlockPos pos = onAsh || clickedState.canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.25), ItemEntity::isAlive);
        int logs = 0, branches = 0;
        for (ItemEntity item : items) {
            if (item.getItem().is(ModTags.THIN_LOGS)) logs += item.getItem().getCount();
            if (item.getItem().is(ModItems.STICK_BRANCH.get())) branches += item.getItem().getCount();
        }
        if (logs < LOGS || branches < BRANCHES) {
            player.displayClientMessage(Component.translatable("message.trailcraft.firestarter.need_fuel",
                    LOGS, TrailCampfireBlock.MAX_LOGS, BRANCHES), true);
            return false;
        }
        BlockPos below = pos.below();
        if (!onAsh && (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))) {
            player.displayClientMessage(Component.translatable("message.trailcraft.firestarter.no_place"), true);
            return false;
        }

        List<ItemStack> taken = new ArrayList<>();
        int needBranches = BRANCHES;
        for (ItemEntity item : items) {
            ItemStack rest = item.getItem().copy();
            if (taken.size() < TrailCampfireBlock.MAX_LOGS && rest.is(ModTags.THIN_LOGS)) {
                int n = Math.min(TrailCampfireBlock.MAX_LOGS - taken.size(), rest.getCount());
                for (int i = 0; i < n; i++) {
                    taken.add(rest.copyWithCount(1));
                }
                rest.shrink(n);
            } else if (needBranches > 0 && rest.is(ModItems.STICK_BRANCH.get())) {
                int n = Math.min(needBranches, rest.getCount());
                rest.shrink(n);
                needBranches -= n;
            } else {
                continue;
            }
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        }
        TrailCampfireBlock.build(level, pos, player.getDirection(), taken);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.firestarter", LOGS, TrailCampfireBlock.MAX_LOGS, BRANCHES)
                .withStyle(ChatFormatting.GRAY));
    }
}

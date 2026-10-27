package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Тканевая заготовка (3 джута и обработанная палка): в одной руке заготовка, в другой обработанная палка, зажать ПКМ —
 * через WEAVE_TICKS получается тканевое полотно. Палка возвращается с шансом STICK_RETURN, иначе ломается.
 */
public class ClothBlankItem extends Item {
    public static final int WEAVE_TICKS = 60;
    public static final float STICK_RETURN = 0.25f;

    public ClothBlankItem(Properties properties) {
        super(properties);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Рука с обработанной палкой, если заготовка в другой. */
    private static InteractionHand stickHand(LivingEntity entity, ItemStack blank) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (entity.getItemInHand(hand) == blank && entity.getItemInHand(other(hand)).is(ModItems.STICK_PROCESSED.get())) {
                return other(hand);
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getItemInHand(other(hand)).is(ModItems.STICK_PROCESSED.get())) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return WEAVE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && remaining % 10 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        InteractionHand stickHand = stickHand(entity, stack);
        if (level.isClientSide || stickHand == null || !(entity instanceof Player player)) {
            return stack;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (level.random.nextFloat() >= STICK_RETURN) {
                player.getItemInHand(stickHand).shrink(1);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
        ItemStack cloth = new ItemStack(ModItems.CLOTH.get());
        if (!player.getInventory().add(cloth)) {
            player.drop(cloth, false);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.cloth_blank").withStyle(ChatFormatting.GRAY));
    }
}

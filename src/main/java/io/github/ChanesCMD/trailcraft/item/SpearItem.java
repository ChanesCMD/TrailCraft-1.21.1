package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModTiers.SpearStats;
import io.github.ChanesCMD.trailcraft.spear.SpearCombat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Копьё — бэкпорт из 1.21.11 (числа в ModTiers.SpearStats, бой в SpearCombat).
 * ЛКМ — укол: только при полной зарядке атаки, бьёт всех на линии взгляда в 2–4,5 блоках, блоки не ломает.
 * Зажатая ПКМ — разгон: после задержки копьё каждый тик бьёт тех, кто перед ним, уроном от относительной скорости.
 * Стадии разгона: 1 — урон, отбрасывание и сброс с седла; 2 — урон и отбрасывание; 3 — только урон; потом копьё опускается.
 * TODO: зачарование «Выпад» не перенесено.
 */
public class SpearItem extends TieredToolItem {
    private final SpearStats stats;

    public SpearItem(Tier tier, SpearStats stats, Item.Properties properties) {
        super(tier, properties);
        this.stats = stats;
    }

    public SpearStats stats() {
        return stats;
    }

    /** Урон и скорость атаки задаются напрямую (базовые у игрока — 1 и 4). Дальность укола считает SpearCombat. */
    public static ItemAttributeModifiers createAttributes(SpearStats stats) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        stats.jabDamage() - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        stats.attackSpeed() - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    /** Стадия разгона после ticksUsed тиков удержания ПКМ: 0 — ещё не бьёт, 1–3 — стадии, 4 — копьё опущено. */
    public int stage(int ticksUsed) {
        int t = ticksUsed - ticks(stats.delay());
        if (t < 0) {
            return 0;
        }
        if (t < ticks(stats.dismountTime())) {
            return 1;
        }
        if (t < ticks(stats.knockbackTime())) {
            return 2;
        }
        return t < ticks(stats.damageTime()) ? 3 : 4;
    }

    /** Копьё не ломает блоки: ЛКМ всегда колет. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        SpearCombat.stopCharge(player);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Разгон заканчивается сам после третьей стадии. */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ticks(stats.delay()) + ticks(stats.damageTime());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide) {
            SpearCombat.chargeTick(entity, stack, this, getUseDuration(stack, entity) - remainingUseDuration);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        SpearCombat.stopCharge(entity);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        SpearCombat.stopCharge(entity);
        return stack;
    }
}

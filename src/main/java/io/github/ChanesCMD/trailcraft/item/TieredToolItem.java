package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

/**
 * Инструмент без особых действий: серп, коса, пила, кусачки.
 * Пока только оружие — бьёт и тратит прочность, как меч.
 * TODO: уточнить, что делают серп, коса, пила и кусачки.
 */
public class TieredToolItem extends TieredItem {
    public TieredToolItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
}

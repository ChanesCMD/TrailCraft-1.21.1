package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Копьё (бэкпорт из новых версий MC): пока оружие ближнего боя с увеличенной дальностью удара.
 * TODO: уточнить механику копья (бросок, разгон, урон с разбега).
 */
public class SpearItem extends TieredToolItem {
    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "spear_reach");

    public SpearItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, float damage, float speed, double reach) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        damage + tier.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID,
                        reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }
}

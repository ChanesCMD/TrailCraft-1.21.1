package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.item.SpearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Поза руки с копьём от третьего лица (константа ArmPose добавляется через META-INF/enumextensions.json).
 * В покое — как у обычного предмета; при разгоне рука неподвижна; при уколе (SpearJabAnim) рука выдвигается вперёд
 * вместо ванильного взмаха.
 */
public final class SpearArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> PROXY =
            new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) SpearArmPose::apply);

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.yRot = 0;
        boolean mainArm = entity.getMainArm() == arm;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float jab = mainArm ? SpearJabAnim.progress(entity, partialTick) : -1;
        boolean charging = entity.isUsingItem() && (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) == mainArm;
        // Рука не качается при ходьбе, иначе качалось бы и копьё
        if (charging && entity.getUseItem().getItem() instanceof SpearItem spear) {
            // Плавно поднимается от стойки до 45° вперёд (модели spear/*_charge_N рассчитаны под этот угол)
            part.xRot = Mth.lerp(spear.chargeProgress(entity.getTicksUsingItem() + partialTick), -Mth.PI / 10, -Mth.PI / 4);
        } else if (jab >= 0) {
            // Рука на 45° вперёд, копьё горизонтально (модель spear/*_jab)
            part.xRot = -Mth.PI / 4;
        } else {
            // Как ArmPose.ITEM
            part.xRot = part.xRot * 0.5f - Mth.PI / 10;
        }
        if (jab >= 0) {
            // Тычок: рука выдвигается вперёд и чуть вниз
            float thrust = SpearJabAnim.thrust(jab);
            part.z -= thrust * 6;
            part.y += thrust * 2;
            part.xRot += thrust * 0.1f;
        }
        if (mainArm) {
            // Ванильный взмах (setupAttackAnimation идёт после позы) не нужен
            model.attackTime = 0;
        }
    }

    public static HumanoidModel.ArmPose get() {
        return PROXY.getValue();
    }

    private SpearArmPose() {}
}

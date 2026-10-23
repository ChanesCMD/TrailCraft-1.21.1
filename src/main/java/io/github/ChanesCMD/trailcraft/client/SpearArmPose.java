package io.github.ChanesCMD.trailcraft.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * Поза руки с копьём от третьего лица (константа ArmPose добавляется через META-INF/enumextensions.json).
 * В покое — как у обычного предмета; при уколе рука выдвигается вперёд вместо ванильного взмаха.
 */
public final class SpearArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> PROXY =
            new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) SpearArmPose::apply);

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        // Как ArmPose.ITEM
        part.xRot = part.xRot * 0.5f - Mth.PI / 10;
        part.yRot = 0;
        if (model.attackTime > 0 && entity.getMainArm() == arm) {
            // Копьё при уколе горизонтально (модель charging), поэтому рука не поворачивается, а выдвигается вперёд
            float thrust = Mth.sin(model.attackTime * Mth.PI);
            part.z -= thrust * 10;
            part.xRot -= thrust * 0.5f;
            // Ванильный взмах (setupAttackAnimation идёт после позы) не нужен
            model.attackTime = 0;
        }
    }

    public static HumanoidModel.ArmPose get() {
        return PROXY.getValue();
    }

    private SpearArmPose() {}
}

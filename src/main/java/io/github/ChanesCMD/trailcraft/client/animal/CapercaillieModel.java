package io.github.ChanesCMD.trailcraft.client.animal;

import com.google.common.collect.ImmutableList;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Глухарь: крупное тело, голова на шее с клювом и «бородой», веер хвоста, отведённый назад, крылья и ноги как у
 * курицы (машут и шагают так же). Развёртка текстуры — scripts/gen_capercaillie.py, менять вместе.
 */
public class CapercaillieModel<T extends Entity> extends AgeableListModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "capercaillie"), "main");

    /** Хвост отведён назад и вверх. */
    private static final float TAIL_PITCH = -0.9f;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CapercaillieModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2, -6, -2, 4, 7, 4), PartPose.offset(0, 13, -5));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(16, 20).addBox(-1, -5, -4, 2, 2, 2), PartPose.ZERO);
        head.addOrReplaceChild("beard",
                CubeListBuilder.create().texOffs(16, 25).addBox(-1, -3, -3, 2, 2, 1), PartPose.ZERO);
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -6, 8, 7, 12), PartPose.offset(0, 16, 0));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(24, 20).addBox(-4, -8, 0, 8, 9, 1), PartPose.offsetAndRotation(0, 14, 5, TAIL_PITCH, 0, 0));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1, 0, -4, 1, 5, 9), PartPose.offset(-4, 13, 0));
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0, 0, -4, 1, 5, 9), PartPose.offset(4, 13, 0));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(24, 32).addBox(-1, 0, -1, 2, 5, 2)
                .texOffs(24, 40).addBox(-1, 4, -3, 2, 1, 2);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-2, 19, 1));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(2, 19, 1));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of(head);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(body, tail, rightWing, leftWing, rightLeg, leftLeg);
    }

    /** Как ChickenModel: ageInTicks — взмах крыльев (CapercaillieRenderer.getBob). */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f + Mth.PI) * 1.4f * limbSwingAmount;
        rightWing.zRot = ageInTicks;
        leftWing.zRot = -ageInTicks;
        // На бегу хвост покачивается
        tail.xRot = TAIL_PITCH + Mth.cos(limbSwing * 0.6662f * 2) * 0.08f * limbSwingAmount;
    }
}

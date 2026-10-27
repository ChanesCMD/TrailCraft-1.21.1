package io.github.ChanesCMD.trailcraft.client.animal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ChanesCMD.trailcraft.animal.FlyingBird;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.goat.Goat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Модель животного из assets/trailcraft/geo/<id>.json (пишет scripts/gen_animals.py вместе с текстурой): коробки
 * частей, у части — роль, по ней она двигается. Роли: head (смотрит), leg_fr/leg_fl/leg_br/leg_bl (шаг четвероногого),
 * leg_r/leg_l (шаг птицы), wing_r/wing_l (взмах), tail (покачивается, у волка — угол хвоста), wool (видна, пока овца
 * не острижена), horn_r/horn_l (у козы, пока рог цел). Лёжа (лиса спит или сидит, волк сидит) модель опускается
 * на lie пикселей, ноги подогнуты.
 */
public class TrailAnimalModel<T extends LivingEntity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final Map<String, List<ModelPart>> roles = new HashMap<>();
    private final float lie;
    /** Взмах крыльев (у птиц — как у курицы и попугая), считает рендер. */
    float flap;

    public TrailAnimalModel(ModelPart root, JsonObject geo) {
        this.root = root;
        this.lie = geo.has("lie") ? geo.get("lie").getAsFloat() : 0;
        for (JsonElement element : geo.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();
            if (part.has("role")) {
                roles.computeIfAbsent(part.get("role").getAsString(), k -> new ArrayList<>()).add(find(root, part.get("name").getAsString()));
            }
        }
    }

    private static ModelPart find(ModelPart part, String name) {
        if (part.hasChild(name)) {
            return part.getChild(name);
        }
        for (ModelPart child : part.getAllParts().toList()) {
            if (child != part && child.hasChild(name)) {
                return child.getChild(name);
            }
        }
        throw new IllegalStateException("No model part " + name);
    }

    public static JsonObject load(String id) {
        String path = "/assets/trailcraft/geo/" + id + ".json";
        try (InputStream in = TrailAnimalModel.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    public static LayerDefinition createLayer(JsonObject geo) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = new HashMap<>();
        PartDefinition top = mesh.getRoot().addOrReplaceChild("model", CubeListBuilder.create(), PartPose.ZERO);
        for (JsonElement element : geo.getAsJsonArray("parts")) {
            JsonObject part = element.getAsJsonObject();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement c : part.getAsJsonArray("cubes")) {
                JsonObject cube = c.getAsJsonObject();
                JsonArray uv = cube.getAsJsonArray("uv"), box = cube.getAsJsonArray("box");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).mirror(cube.has("mirror") && cube.get("mirror").getAsBoolean())
                        .addBox(f(box, 0), f(box, 1), f(box, 2), f(box, 3), f(box, 4), f(box, 5));
            }
            JsonArray pivot = part.getAsJsonArray("pivot");
            JsonArray rot = part.has("rotation") ? part.getAsJsonArray("rotation") : null;
            PartPose pose = PartPose.offsetAndRotation(f(pivot, 0), f(pivot, 1), f(pivot, 2),
                    rot == null ? 0 : f(rot, 0), rot == null ? 0 : f(rot, 1), rot == null ? 0 : f(rot, 2));
            PartDefinition parent = part.has("parent") ? parts.get(part.get("parent").getAsString()) : top;
            parts.put(part.get("name").getAsString(), parent.addOrReplaceChild(part.get("name").getAsString(), cubes, pose));
        }
        JsonArray size = geo.getAsJsonArray("texture");
        return LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
    }

    private static float f(JsonArray array, int i) {
        return array.get(i).getAsFloat();
    }

    @Override
    public ModelPart root() {
        return root;
    }

    private List<ModelPart> role(String name) {
        return roles.getOrDefault(name, List.of());
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean lying = entity instanceof Fox fox && (fox.isSleeping() || fox.isSitting())
                || entity instanceof TamableAnimal tame && tame.isInSittingPose();
        float walk = Mth.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        for (ModelPart head : role("head")) {
            head.xRot += headPitch * Mth.DEG_TO_RAD + (entity instanceof Goat goat ? goat.getRammingXHeadRot() : 0)
                    + (entity instanceof Fox fox && fox.isSleeping() ? 0.5f : 0);
            head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
            // масштаб resetPose не сбрасывает, а модель одна на всех животных вида
            head.xScale = head.yScale = head.zScale = young ? 1.4f : 1;
        }
        for (String leg : new String[] {"leg_fr", "leg_bl", "leg_r"}) {
            role(leg).forEach(part -> part.xRot += walk);
        }
        for (String leg : new String[] {"leg_fl", "leg_br", "leg_l"}) {
            role(leg).forEach(part -> part.xRot -= walk);
        }
        role("wing_r").forEach(part -> part.zRot += flap);
        role("wing_l").forEach(part -> part.zRot -= flap);
        for (ModelPart tail : role("tail")) {
            if (entity instanceof Wolf wolf) {
                tail.xRot = wolf.getTailAngle();
            }
            tail.yRot += Mth.cos(limbSwing * 0.6662f) * 0.3f * limbSwingAmount;
        }
        if (entity instanceof FlyingBird bird && bird.isFlying()) {
            // в полёте лапы поджаты назад
            role("leg_r").forEach(part -> part.xRot = 1.2f);
            role("leg_l").forEach(part -> part.xRot = 1.2f);
        }
        if (entity instanceof Sheep sheep) {
            role("wool").forEach(part -> part.visible = !sheep.isSheared());
        }
        if (entity instanceof Goat goat) {
            role("horn_l").forEach(part -> part.visible = goat.hasLeftHorn());
            role("horn_r").forEach(part -> part.visible = goat.hasRightHorn());
        }
        if (lying) {
            root.getChild("model").y += lie;
            role("leg_fr").forEach(part -> part.xRot = -Mth.HALF_PI);
            role("leg_fl").forEach(part -> part.xRot = -Mth.HALF_PI);
            role("leg_br").forEach(part -> part.xRot = Mth.HALF_PI);
            role("leg_bl").forEach(part -> part.xRot = Mth.HALF_PI);
        }
    }

    /** Детёныш — вдвое меньше (голова крупнее, см. setupAnim), ноги на земле. */
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        if (!young) {
            super.renderToBuffer(poseStack, buffer, light, overlay, color);
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, 1.5 * 0.5, 0);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        super.renderToBuffer(poseStack, buffer, light, overlay, color);
        poseStack.popPose();
    }

    /** Птицы машут крыльями, как курица (Chicken.flap) и попугай (FlyingBird.flap). */
    static float flap(LivingEntity entity, float partialTick) {
        float flap, speed;
        if (entity instanceof Chicken chicken) {
            flap = Mth.lerp(partialTick, chicken.oFlap, chicken.flap);
            speed = Mth.lerp(partialTick, chicken.oFlapSpeed, chicken.flapSpeed);
        } else if (entity instanceof FlyingBird bird) {
            flap = Mth.lerp(partialTick, bird.oFlap, bird.flap);
            speed = Mth.lerp(partialTick, bird.oFlapSpeed, bird.flapSpeed);
        } else {
            return 0;
        }
        return (Mth.sin(flap) + 1) * speed;
    }
}

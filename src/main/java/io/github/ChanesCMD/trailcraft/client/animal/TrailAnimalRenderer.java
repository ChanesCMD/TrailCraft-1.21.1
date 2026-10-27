package io.github.ChanesCMD.trailcraft.client.animal;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.animal.Species;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Рендер животного TrailCraft: модель TrailAnimalModel и текстура textures/entity/<id>.png. */
public class TrailAnimalRenderer<T extends Mob> extends MobRenderer<T, TrailAnimalModel<T>> {
    private final ResourceLocation texture;

    public TrailAnimalRenderer(EntityRendererProvider.Context context, Species species) {
        super(context, model(context, species), species.width * 0.6f);
        this.texture = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "textures/entity/" + species.id + ".png");
    }

    private static <T extends Mob> TrailAnimalModel<T> model(EntityRendererProvider.Context context, Species species) {
        JsonObject geo = TrailAnimalModel.load(species.id);
        return new TrailAnimalModel<>(context.bakeLayer(layer(species)), geo);
    }

    public static ModelLayerLocation layer(Species species) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, species.id), "main");
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        getModel().flap = TrailAnimalModel.flap(entity, partialTick);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}

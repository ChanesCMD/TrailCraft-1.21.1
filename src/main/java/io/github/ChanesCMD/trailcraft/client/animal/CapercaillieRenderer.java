package io.github.ChanesCMD.trailcraft.client.animal;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.animal.Capercaillie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CapercaillieRenderer extends MobRenderer<Capercaillie, CapercaillieModel<Capercaillie>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "textures/entity/capercaillie.png");

    public CapercaillieRenderer(EntityRendererProvider.Context context) {
        super(context, new CapercaillieModel<>(context.bakeLayer(CapercaillieModel.LAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(Capercaillie entity) {
        return TEXTURE;
    }

    /** Взмах крыльев, как у ChickenRenderer. */
    @Override
    protected float getBob(Capercaillie entity, float partialTick) {
        float flap = Mth.lerp(partialTick, entity.oFlap, entity.flap);
        float speed = Mth.lerp(partialTick, entity.oFlapSpeed, entity.flapSpeed);
        return (Mth.sin(flap) + 1) * speed;
    }
}

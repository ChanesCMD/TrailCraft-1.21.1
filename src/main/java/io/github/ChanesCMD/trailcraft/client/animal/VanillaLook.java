package io.github.ChanesCMD.trailcraft.client.animal;

import io.github.ChanesCMD.trailcraft.animal.Species;
import io.github.ChanesCMD.trailcraft.init.ModAnimals;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Виды с ванильным аналогом рисуются ванильными рендерами, моделями и текстурами (курица, свинья, корова, овца, коза,
 * волк, лиса, песец — снежная лиса, заяц — ванильный кролик). Их сущности (animal/Trail*) наследуют ванильные классы,
 * поэтому ванильные рендеры подходят как есть. Остальные виды рисует TrailAnimalRenderer со своими моделями
 * (textures/entity/&lt;id&gt;.png и geo-модель, их пишет scripts/gen_animals.py).
 */
public final class VanillaLook {
    private static final Set<Species> SPECIES = EnumSet.of(Species.CHICKEN, Species.PIG, Species.COW, Species.SHEEP,
            Species.GOAT, Species.WOLF, Species.FOX, Species.ARCTIC_FOX, Species.HARE);

    private static final ResourceLocation SNOW_FOX = ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox.png");
    private static final ResourceLocation SNOW_FOX_SLEEP = ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox_sleep.png");

    /** Рисуется ли вид ванильной моделью. */
    public static boolean has(Species species) {
        return SPECIES.contains(species);
    }

    /** Регистрирует ванильный рендер для вида (только для видов, у которых has == true). */
    public static void register(EntityRenderersEvent.RegisterRenderers e, Species species) {
        switch (species) {
            case CHICKEN -> e.<Chicken>registerEntityRenderer(as(species), ChickenRenderer::new);
            case PIG -> e.<Pig>registerEntityRenderer(as(species), PigRenderer::new);
            case COW -> e.<Cow>registerEntityRenderer(as(species), CowRenderer::new);
            case SHEEP -> e.<Sheep>registerEntityRenderer(as(species), SheepRenderer::new);
            case GOAT -> e.<Goat>registerEntityRenderer(as(species), GoatRenderer::new);
            case WOLF -> e.<Wolf>registerEntityRenderer(as(species), WolfRenderer::new);
            case FOX -> e.<Fox>registerEntityRenderer(as(species), FoxRenderer::new);
            case ARCTIC_FOX -> e.<Fox>registerEntityRenderer(as(species), SnowFoxRenderer::new);
            case HARE -> e.<Rabbit>registerEntityRenderer(as(species), RabbitRenderer::new);
            default -> throw new IllegalArgumentException("Нет ванильной модели для " + species);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<? extends T> as(Species species) {
        return (EntityType<? extends T>) ModAnimals.type(species);
    }

    /** Песец: ванильная лиса с текстурой снежной лисы (у спящей — со своей). */
    private static class SnowFoxRenderer extends FoxRenderer {
        SnowFoxRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(Fox fox) {
            return fox.isSleeping() ? SNOW_FOX_SLEEP : SNOW_FOX;
        }
    }

    private VanillaLook() {}
}

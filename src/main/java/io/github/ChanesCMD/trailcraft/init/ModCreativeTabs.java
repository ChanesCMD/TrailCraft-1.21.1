package io.github.ChanesCMD.trailcraft.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.ChanesCMD.trailcraft.TrailCraft;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrailCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILCRAFT_TAB =
            CREATIVE_MODE_TABS.register("trailcraft_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trailcraft"))
                    .icon(() -> ModBlocks.ANDESITE.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Камни (12)
                        output.accept(ModBlocks.ANDESITE);
                        output.accept(ModBlocks.BASALT);
                        output.accept(ModBlocks.CHALK);
                        output.accept(ModBlocks.DIORITE);
                        output.accept(ModBlocks.DOLOMITE);
                        output.accept(ModBlocks.GABBRO);
                        output.accept(ModBlocks.GRANITE);
                        output.accept(ModBlocks.GRAY_GRANITE);
                        output.accept(ModBlocks.LIMESTONE);
                        output.accept(ModBlocks.MARBLE);
                        output.accept(ModBlocks.SANDSTONE);
                        output.accept(ModBlocks.SHALE);

                        // Базовые блоки (8)
                        output.accept(ModBlocks.ZHELTOZEM);
                        output.accept(ModBlocks.BUROZEM);
                        output.accept(ModBlocks.CHERNOZEM);
                        output.accept(ModBlocks.LOAM);
                        output.accept(ModBlocks.CLAY_WHITE);
                        output.accept(ModBlocks.CLAY_BLUE);
                        output.accept(ModBlocks.CLAY_GRAY);
                        output.accept(ModBlocks.PEAT);

                        // Травяные пары (8)
                        output.accept(ModBlocks.ZHELTOZEM_GRASS);
                        output.accept(ModBlocks.BUROZEM_GRASS);
                        output.accept(ModBlocks.CHERNOZEM_GRASS);
                        output.accept(ModBlocks.LOAM_GRASS);
                        output.accept(ModBlocks.CLAY_WHITE_GRASS);
                        output.accept(ModBlocks.CLAY_BLUE_GRASS);
                        output.accept(ModBlocks.CLAY_GRAY_GRASS);
                        output.accept(ModBlocks.PEAT_GRASS);

                        // Песок (3)
                        output.accept(ModBlocks.SAND_WHITE);
                        output.accept(ModBlocks.SAND_YELLOW);
                        output.accept(ModBlocks.SAND_DARK);

                        // Ил (1)
                        output.accept(ModBlocks.SILT);

                        // Трава (3)
                        output.accept(ModBlocks.GRASS_MEDIUM);
                        output.accept(ModBlocks.GRASS_SMALL);
                        output.accept(ModBlocks.GRASS_TALL);
                    }).build());
}
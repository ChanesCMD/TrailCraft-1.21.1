package io.github.ChanesCMD.trailcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import io.github.ChanesCMD.trailcraft.init.ModChests;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModEffects;
import io.github.ChanesCMD.trailcraft.init.ModAnimals;
import io.github.ChanesCMD.trailcraft.init.ModEntities;
import io.github.ChanesCMD.trailcraft.init.ModSounds;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModCreativeTabs;
import io.github.ChanesCMD.trailcraft.init.ModFeatures;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
import io.github.ChanesCMD.trailcraft.init.ModOres;
import io.github.ChanesCMD.trailcraft.init.ModParticles;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.init.ModBees;
import io.github.ChanesCMD.trailcraft.init.ModFluids;
import io.github.ChanesCMD.trailcraft.init.ModTools;
import io.github.ChanesCMD.trailcraft.init.ModWood;
import io.github.ChanesCMD.trailcraft.loot.ReplaceItemModifier;

@Mod(TrailCraft.MODID)
public class TrailCraft {
    public static final String MODID = "trailcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrailCraft(IEventBus modEventBus, ModContainer modContainer) {
        // Деревья регистрируются в статическом блоке ModWood
        ModWood.init();
        // Сундуки по породам — после деревьев (init/ModChests)
        ModChests.init();
        // Инструменты регистрируются в статическом блоке ModTools
        ModTools.init();
        // Руды, слитки и шихта регистрируются в статическом блоке ModOres
        ModOres.init();
        // Глина, кирпичи, сыродутная печь и кувшин (пакет smelting)
        ModSmelting.init();
        // Животные (animal/Species) регистрируются в статическом блоке ModAnimals
        ModAnimals.init();
        // Пчёлы, ульи и пасека (пакет bee)
        ModBees.init();
        // Креозот и танин (жидкости, их блоки и вёдра)
        ModFluids.init();
        // Укреплённые жидкостные детали Create (init/ModReinforced)
        io.github.ChanesCMD.trailcraft.init.ModReinforced.init();

        // Регистрируем блоки и предметы
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ReplaceItemModifier.SERIALIZERS.register(modEventBus);

        // Регистрируем конфиг
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // Серверный конфиг (календарь): лежит в папке мира, клиентам синхронизируется
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
package io.github.ChanesCMD.trailcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import io.github.ChanesCMD.trailcraft.init.ModBlocks;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.init.ModCreativeTabs;
import io.github.ChanesCMD.trailcraft.init.ModFeatures;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.init.ModWood;

@Mod(TrailCraft.MODID)
public class TrailCraft {
    public static final String MODID = "trailcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrailCraft(IEventBus modEventBus, ModContainer modContainer) {
        // Деревья регистрируются в статическом блоке ModWood
        ModWood.init();

        // Регистрируем блоки и предметы
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);

        // Регистрируем конфиг
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // Серверный конфиг (календарь): лежит в папке мира, клиентам синхронизируется
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
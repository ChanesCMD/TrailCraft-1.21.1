package io.github.ChanesCMD.trailcraft;

import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.client.ClientOres;
import io.github.ChanesCMD.trailcraft.client.DevInstance;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.nio.file.Path;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = TrailCraft.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class TrailCraftClient {
    public TrailCraftClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Только запуск из среды разработки (play.bat): у игроков этого свойства нет
        String pidFile = System.getProperty("trailcraft.pidFile");
        if (pidFile != null) {
            DevInstance.register(Path.of(pidFile));
        }
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientOres::registerItemProperties);
        // Some client setup code
        TrailCraft.LOGGER.info("HELLO FROM CLIENT SETUP");
        TrailCraft.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}

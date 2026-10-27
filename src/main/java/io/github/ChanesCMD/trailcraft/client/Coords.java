package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

/**
 * Координаты, которые знает игрок (HUD, карта, F3). Без приборов — номер сектора 9×9 чанков (144×144 блока):
 * «HX 3 HZ -2», высоты нет. Приборы на поясе (ClientGear): секстант уточняет X до чанка («MX 32» — номер чанка
 * от нуля), хронометр так же Z («MZ -13»), высотомер показывает высоту полосой по 8 блоков («Y 64...71»).
 * GPS на поясе — ванильные координаты.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class Coords {
    public static final int SECTOR = 9 * 16, CHUNK = 16, HEIGHT_BAND = 8;

    /** Строка координат игрока: X, высота, Z. */
    public static String of(int x, int y, int z) {
        if (ClientGear.hasGps()) {
            return "X " + x + " Y " + y + " Z " + z;
        }
        String line = axis("X", x, ClientGear.hasSextant());
        if (ClientGear.hasAltimeter()) {
            line += " " + height(y);
        }
        return line + " " + axis("Z", z, ClientGear.hasChronometer());
    }

    /** Точка на карте: только X и Z. */
    public static String horizontal(int x, int z) {
        if (ClientGear.hasGps()) {
            return "X " + x + " Z " + z;
        }
        return axis("X", x, ClientGear.hasSextant()) + " " + axis("Z", z, ClientGear.hasChronometer());
    }

    /** «MX 32» — номер чанка, если есть прибор этой оси, иначе «HX 3» — номер сектора. */
    private static String axis(String name, int block, boolean precise) {
        return precise ? "M" + name + " " + Math.floorDiv(block, CHUNK) : "H" + name + " " + Math.floorDiv(block, SECTOR);
    }

    /** Полоса высоты: «Y 64...71». */
    private static String height(int y) {
        int low = Math.floorDiv(y, HEIGHT_BAND) * HEIGHT_BAND;
        return "Y " + low + "..." + (low + HEIGHT_BAND - 1);
    }

    /**
     * Экран F3: без GPS строка XYZ показывает то же, что HUD, а строки, выдающие точное место (блок, чанк, высоты
     * поверхности, координаты блока под прицелом), убираются.
     */
    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ClientGear.hasGps()) {
            return;
        }
        List<String> left = event.getLeft();
        left.replaceAll(line -> line.startsWith("XYZ: ")
                ? "XYZ: " + of(player.getBlockX(), player.getBlockY(), player.getBlockZ()) : line);
        left.removeIf(line -> line.startsWith("Block: ") || line.startsWith("Chunk: ")
                || line.startsWith("CH ") || line.startsWith("SH "));
        // «Targeted Block: x, y, z» (с кодом подчёркивания в начале) — оставляем заголовок без координат
        event.getRight().replaceAll(line -> line.contains("Targeted Block: ") || line.contains("Targeted Fluid: ")
                ? line.substring(0, line.indexOf(": ")) : line);
    }

    private Coords() {}
}

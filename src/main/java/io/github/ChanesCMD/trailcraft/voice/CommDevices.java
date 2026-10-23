package io.github.ChanesCMD.trailcraft.voice;

import io.github.ChanesCMD.trailcraft.ServerConfig;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Устройства связи (рация, радио, телефон) — предметы с тегом #trailcraft:comm_devices.
 * Если на сервере включён флаг voiceRequiresDevice, без устройства доступен только локальный голосовой чат.
 */
public final class CommDevices {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "comm_devices"));

    /**
     * Есть ли у игрока устройство связи. TODO: по ТЗ (3.2) оно лежит в слоте «Связь» на поясе; окна пояса ещё нет,
     * поэтому пока ищем в хотбаре и левой руке. Предметов с этим тегом пока тоже нет.
     */
    public static boolean has(Player player) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (player.getInventory().getItem(i).is(TAG)) {
                return true;
            }
        }
        return player.getOffhandItem().is(TAG);
    }

    /** Флаг voiceRequiresDevice, присланный сервером клиенту (меняется без перезахода); null — ещё не приходил. */
    private static Boolean clientRule;

    public static void setClientRule(boolean requiresDevice) {
        clientRule = requiresDevice;
    }

    /** Доступны ли игроку каналы party/city/country и звонки. */
    public static boolean allowsRemoteVoice(Player player) {
        boolean required = player.level().isClientSide && clientRule != null ? clientRule : ServerConfig.voiceRequiresDevice();
        return !required || has(player);
    }

    private CommDevices() {}
}

package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.inventory.CosmeticPayload;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Косметика игроков (присылает сервер, см. CosmeticPayload). На время отрисовки игрока броня в его инвентаре
 * на клиенте подменяется косметикой, а после — возвращается: так её рисует ванильный слой брони.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class ClientCosmetics {
    private static final Map<Integer, List<ItemStack>> SHOWN = new HashMap<>();
    /** Броня, снятая на время отрисовки (индексы Inventory.armor: 0 — ботинки … 3 — шлем). */
    private static final ItemStack[] saved = new ItemStack[4];
    private static Player rendered;

    public static void set(CosmeticPayload payload) {
        SHOWN.put(payload.entityId(), payload.stacks());
    }

    /** Самый низкий приоритет: если отрисовку отменил другой мод, Post не придёт, и подменять нечего. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        restore();
        List<ItemStack> shown = SHOWN.get(event.getEntity().getId());
        if (shown == null) {
            return;
        }
        Player player = event.getEntity();
        NonNullList<ItemStack> armor = player.getInventory().armor;
        for (int i = 0; i < 4 && i < shown.size(); i++) {
            if (!shown.get(i).isEmpty()) {
                int slot = 3 - i;
                saved[slot] = armor.get(slot);
                armor.set(slot, shown.get(i));
                rendered = player;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        restore();
    }

    private static void restore() {
        if (rendered == null) {
            return;
        }
        NonNullList<ItemStack> armor = rendered.getInventory().armor;
        for (int slot = 0; slot < 4; slot++) {
            if (saved[slot] != null) {
                armor.set(slot, saved[slot]);
                saved[slot] = null;
            }
        }
        rendered = null;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restore();
        SHOWN.clear();
    }

    private ClientCosmetics() {}
}

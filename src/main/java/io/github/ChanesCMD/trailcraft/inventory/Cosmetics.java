package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Косметика: если переключатель слота включён и в косметическом слоте что-то лежит, другие игроки (и сам игрок)
 * видят этот предмет вместо надетой брони. Заодно — какой рюкзак надет (его рисует на спине client/BackpackLayer).
 * Слоты игрока на клиенты не синхронизируются — шлём CosmeticPayload.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class Cosmetics {
    /**
     * Что показывать вместо брони в слотах 0–3 (шлем … ботинки; пустой предмет — саму броню) и в 4 — надетый рюкзак
     * (только сам предмет, без содержимого: другим игрокам оно ни к чему).
     */
    public static List<ItemStack> shown(ServerPlayer player) {
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        int toggles = player.getData(ModAttachments.COSMETIC_TOGGLES);
        List<ItemStack> shown = new ArrayList<>(CosmeticPayload.SIZE);
        for (int i = 0; i < 4; i++) {
            shown.add((toggles & (1 << i)) != 0 ? slots.getStackInSlot(PlayerSlots.COSMETIC + i).copy() : ItemStack.EMPTY);
        }
        shown.add(backpack(slots));
        return shown;
    }

    private static ItemStack backpack(PlayerSlots slots) {
        ItemStack worn = slots.getStackInSlot(PlayerSlots.BACKPACK);
        return worn.isEmpty() ? ItemStack.EMPTY : new ItemStack(worn.getItem());
    }

    /**
     * Каждый тик: если показываемая косметика изменилась, рассылаем её игроку и всем, кто его видит.
     * Сравниваем без копирования, копии делаем только при изменении.
     */
    static void sync(ServerPlayer player) {
        if (!changed(player)) {
            return;
        }
        List<ItemStack> shown = shown(player);
        player.setData(ModAttachments.COSMETIC_SENT, shown);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new CosmeticPayload(player.getId(), shown));
    }

    private static boolean changed(ServerPlayer player) {
        List<ItemStack> sent = player.getData(ModAttachments.COSMETIC_SENT);
        if (sent.size() != CosmeticPayload.SIZE) {
            return true;
        }
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        int toggles = player.getData(ModAttachments.COSMETIC_TOGGLES);
        for (int i = 0; i < 4; i++) {
            ItemStack shown = (toggles & (1 << i)) != 0 ? slots.getStackInSlot(PlayerSlots.COSMETIC + i) : ItemStack.EMPTY;
            if (!ItemStack.matches(shown, sent.get(i))) {
                return true;
            }
        }
        return !ItemStack.matches(backpack(slots), sent.get(CosmeticPayload.BACKPACK));
    }

    /** Игрок появился в зоне видимости другого — тому нужна его косметика. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new CosmeticPayload(target.getId(), shown(target)));
        }
    }

    private Cosmetics() {}
}

package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.BackpackBlock;
import io.github.ChanesCMD.trailcraft.block.GraveBlock;
import io.github.ChanesCMD.trailcraft.client.ClientInventoryEvents;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TrailCraft.MODID)
public class InventoryEvents {
    /** Ванильные слоты 9..35 не используются: основной инвентарь лежит в рюкзаке. */
    private static final int VANILLA_MAIN_START = 9, VANILLA_MAIN_END = 36;

    /**
     * Смерть без keepInventory: вместо выпадения вещей ставится могила. В неё идут все вещи игрока
     * (рюкзак — со всем содержимым) и монеты из кошелька. Пояс с содержимым и сам кошелёк остаются у игрока.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        List<ItemStack> loot = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            loot.add(drop.getItem());
        }
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        for (int i = 0; i < slots.getSlots(); i++) {
            if (i == PlayerSlots.BELT || i == PlayerSlots.WALLET) {
                continue;
            }
            ItemStack stack = slots.getStackInSlot(i);
            slots.setStackInSlot(i, ItemStack.EMPTY);
            if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                loot.add(stack);
            }
        }
        ItemStack wallet = slots.getStackInSlot(PlayerSlots.WALLET);
        ItemContainerContents coins = wallet.get(ModComponents.CONTENTS.get());
        if (coins != null) {
            coins.nonEmptyItemsCopy().forEach(loot::add);
            wallet.remove(ModComponents.CONTENTS.get());
        }

        long now = System.currentTimeMillis();
        boolean hadBackpack = loot.stream().anyMatch(stack -> stack.getItem() instanceof BackpackItem);
        BlockPos gravePos = loot.isEmpty() ? null : GraveBlock.place(player, loot, now);
        event.getDrops().clear();
        if (gravePos == null) {
            // Места под могилу нет — вещи выпадают как обычно
            for (ItemStack stack : loot) {
                ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
                entity.setDefaultPickUpDelay();
                event.getDrops().add(entity);
            }
        }
        DeathRecord.add(player, new DeathRecord(now, player.level().getDayTime(), player.level().dimension().location().toString(),
                gravePos != null ? gravePos : player.blockPosition(), hadBackpack && gravePos != null, false));
    }

    /** Выброшенный рюкзак ставится на землю блоком. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        Player player = event.getPlayer();
        if (stack.getItem() instanceof BackpackItem && !player.level().isClientSide
                && BackpackBlock.placeOnGround(player.level(), player.blockPosition(), stack)) {
            event.setCanceled(true);
        }
    }

    /** Не подбирать то, чему нет места ни в хотбаре, ни в рюкзаке (иначе предмет упадёт в закрытые слоты). */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (!player.isCreative() && !player.isSpectator() && !canFit(player, event.getItemEntity().getItem())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /** Всё, что попало в ванильные слоты 9..35 (подбор, /give, другие моды), переносится в рюкзак или выбрасывается. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        Inventory inventory = player.getInventory();
        BackpackHandler backpack = null;
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (backpack == null) {
                backpack = new BackpackHandler(player);
            }
            inventory.setItem(i, ItemStack.EMPTY);
            ItemStack rest = backpack.insertMain(stack, false);
            if (!rest.isEmpty()) {
                player.drop(rest, false);
            }
        }
    }

    private static boolean canFit(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (fits(inventory.getItem(i), stack)) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && fits(offhand, stack)) {
            return true;
        }
        return new BackpackHandler(player).insertMain(stack, true).getCount() < stack.getCount();
    }

    private static boolean fits(ItemStack slot, ItemStack stack) {
        return slot.isEmpty() || ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize();
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(OpenInventoryPayload.TYPE, OpenInventoryPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            open(player);
                        }
                    }));
            registrar.playToServer(DeathLogPayloads.Request.TYPE, DeathLogPayloads.Request.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            PacketDistributor.sendToPlayer(player, new DeathLogPayloads.Log(
                                    player.getData(ModAttachments.DEATH_LOG), System.currentTimeMillis()));
                        }
                    }));
            registrar.playToClient(DeathLogPayloads.Log.TYPE, DeathLogPayloads.Log.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> ClientInventoryEvents.showDeathLog(payload)));
        }
    }

    /** Открывает инвентарь, только если у игрока не открыт другой контейнер. Креатив пользуется ванильным. */
    private static void open(ServerPlayer player) {
        if (player.containerMenu == player.inventoryMenu && !player.isCreative() && !player.isSpectator()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TrailInventoryMenu(id, inventory),
                    Component.translatable("container.trailcraft.inventory")));
        }
    }
}

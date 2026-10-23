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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TrailCraft.MODID)
public class InventoryEvents {
    /** Ванильные слоты 9..35 не используются: основной инвентарь лежит в рюкзаке. */
    private static final int VANILLA_MAIN_START = 9, VANILLA_MAIN_END = 36;

    /**
     * Хотбар при смерти остаётся у игрока: откладываем его до того, как ванилла выбросит инвентарь,
     * и возвращаем после возрождения (onClone). Предметы с проклятием утраты не откладываем — ванилла их уничтожит.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || keepsInventory(player)) {
            return;
        }
        ItemStackHandler kept = player.getData(ModAttachments.KEPT_HOTBAR);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                kept.setStackInSlot(i, stack);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        ItemStackHandler kept = event.getOriginal().getData(ModAttachments.KEPT_HOTBAR);
        Player player = event.getEntity();
        for (int i = 0; i < kept.getSlots(); i++) {
            ItemStack stack = kept.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            kept.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    /**
     * Смерть без keepInventory: вместо выпадения вещей ставится могила. В неё идут броня, левая рука, аксессуары,
     * рюкзак со всем содержимым и монеты из кошелька. Хотбар (см. onDeath), тёплая одежда, косметика, пояс
     * с содержимым и сам кошелёк остаются у игрока. Каждая смерть записывается в журнал.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (keepsInventory(player)) {
            DeathRecord.add(player, record(player, now, player.blockPosition(), false));
            return;
        }
        List<ItemStack> loot = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            loot.add(drop.getItem());
        }
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        for (int i = 0; i < slots.getSlots(); i++) {
            if (i == PlayerSlots.BELT || i == PlayerSlots.WALLET || i < PlayerSlots.ACCESSORIES) {
                continue; // косметика (0–3) и тёплая одежда (4–7) остаются
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

        boolean hadBackpack = loot.stream().anyMatch(stack -> stack.getItem() instanceof BackpackItem);
        // Могила ставится при любой смерти, даже без рюкзака и пустая: по ней видно место смерти
        BlockPos gravePos = GraveBlock.place(player, loot, now);
        event.getDrops().clear();
        if (gravePos == null) {
            // Места под могилу нет — вещи выпадают как обычно
            for (ItemStack stack : loot) {
                ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
                entity.setDefaultPickUpDelay();
                event.getDrops().add(entity);
            }
        }
        DeathRecord.add(player, record(player, now, gravePos != null ? gravePos : player.blockPosition(), hadBackpack && gravePos != null));
    }

    private static DeathRecord record(ServerPlayer player, long now, BlockPos pos, boolean hadBackpack) {
        return new DeathRecord(now, player.level().getDayTime(), player.level().dimension().location().toString(), pos, hadBackpack, false);
    }

    private static boolean keepsInventory(Player player) {
        return player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
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

    /**
     * Рюкзак при подборе сразу надевается (в хотбар он не кладётся). Остальное не подбирается,
     * если места нет ни в хотбаре, ни в рюкзаке (иначе предмет упадёт в закрытые слоты).
     */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (stack.getItem() instanceof BackpackItem) {
            event.setCanPickup(TriState.FALSE);
            if (!entity.hasPickUpDelay() && BackpackItem.equip(player, stack.copy())) {
                player.take(entity, stack.getCount());
                entity.discard();
            }
        } else if (!canFit(player, stack)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /**
     * Рюкзак, попавший в хотбар или ванильные слоты (обмен клавишами, /give, другие моды), надевается,
     * а если некуда — ставится на землю. Всё остальное из ванильных слотов 9..35 переносится в рюкзак или выбрасывается.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        Inventory inventory = player.getInventory();
        BackpackHandler backpack = null;
        for (int i = 0; i < VANILLA_MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BackpackItem) {
                inventory.setItem(i, ItemStack.EMPTY);
                if (!BackpackItem.equip(player, stack)) {
                    player.drop(stack, false);
                }
                continue;
            }
            if (stack.isEmpty() || i < VANILLA_MAIN_START) {
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
                            PacketDistributor.sendToPlayer(player, new DeathLogPayloads.Log(player.getData(ModAttachments.DEATH_LOG),
                                    player.getData(ModAttachments.DEATH_COUNT), System.currentTimeMillis()));
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

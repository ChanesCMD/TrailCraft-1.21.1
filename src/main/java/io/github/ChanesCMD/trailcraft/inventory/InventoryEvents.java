package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = TrailCraft.MODID)
public class InventoryEvents {

    /** Без keepInventory дополнительные слоты выпадают вместе с ванильным инвентарём. */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        PlayerSlots slots = player.getData(ModAttachments.PLAYER_SLOTS);
        for (int i = 0; i < slots.getSlots(); i++) {
            ItemStack stack = slots.getStackInSlot(i);
            slots.setStackInSlot(i, ItemStack.EMPTY);
            if (stack.isEmpty() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                continue;
            }
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
            entity.setDefaultPickUpDelay();
            event.getDrops().add(entity);
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(OpenInventoryPayload.TYPE, OpenInventoryPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            open(player);
                        }
                    }));
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

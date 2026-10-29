package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.FlintKnappingScreen;
import io.github.ChanesCMD.trailcraft.init.ModSmelting;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

/**
 * Кремень: кладётся на землю камешком (блок flint_pebble), а ПКМ в правой руке по породе (#trailcraft:rocks, без Shift)
 * открывает окно оббивки, как у слепка из глины: выбранная кремнёвая головка (ModSmelting.FLINT_TOOLS) заменяет один
 * кремень. Головка над веткой или обработанной палкой — инструмент (рецепты — scripts/gen_casting.py).
 */
public class FlintItem extends BlockItem {
    public FlintItem(Block pebble, Item.Properties properties) {
        super(pebble, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getHand() != InteractionHand.MAIN_HAND || context.isSecondaryUseActive()
                || !level.getBlockState(context.getClickedPos()).is(ModTags.ROCKS)) {
            return super.useOn(context);
        }
        if (level.isClientSide) {
            FlintKnappingScreen.open();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.flint").withStyle(ChatFormatting.GRAY));
    }

    /** Головка, которую оббивают из кремня (номер в ModSmelting.FLINT_TOOLS). */
    public static ItemStack head(int tool) {
        return new ItemStack(ModSmelting.HEADS.get("flint_" + ModSmelting.FLINT_TOOLS.get(tool).id).get());
    }

    /** Сервер: заменить кремень в правой руке головкой. */
    static void knap(ServerPlayer player, int tool) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FlintItem)) {
            return;
        }
        ItemStack result = head(tool);
        if (held.getCount() == 1) {
            player.setItemInHand(InteractionHand.MAIN_HAND, result);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /** Клиент → сервер: оббить головку (номер в ModSmelting.FLINT_TOOLS). */
    public record KnapPayload(int tool) implements CustomPacketPayload {
        public static final Type<KnapPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "flint_knap"));
        public static final StreamCodec<ByteBuf, KnapPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, KnapPayload::tool, KnapPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(KnapPayload.TYPE, KnapPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player && payload.tool() >= 0
                                && payload.tool() < ModSmelting.FLINT_TOOLS.size()) {
                            knap(player, payload.tool());
                        }
                    }));
        }
    }
}

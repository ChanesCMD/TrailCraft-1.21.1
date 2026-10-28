package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.gui.ClayMoldScreen;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

/**
 * Слепок из глины (как лепка в TerraFirmaCraft): ПКМ в правой руке — окно выбора формы (ClayShape); выбранная форма
 * заменяет один слепок (сырой кувшин или сырая форма для литья, их обжигают на костре).
 */
public class ClayMoldItem extends Item {
    public ClayMoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            ClayMoldScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trailcraft.clay_mold").withStyle(ChatFormatting.GRAY));
    }

    /** Сервер: заменить слепок в правой руке выбранной формой. */
    static void shape(ServerPlayer player, ClayShape shape) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ClayMoldItem)) {
            return;
        }
        ItemStack result = new ItemStack(shape.result());
        if (held.getCount() == 1) {
            player.setItemInHand(InteractionHand.MAIN_HAND, result);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    /** Клиент → сервер: вылепить форму (номер в ClayShape). */
    public record ShapePayload(int shape) implements CustomPacketPayload {
        public static final Type<ShapePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "clay_shape"));
        public static final StreamCodec<ByteBuf, ShapePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ShapePayload::shape, ShapePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(ShapePayload.TYPE, ShapePayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        ClayShape[] shapes = ClayShape.values();
                        if (context.player() instanceof ServerPlayer player && payload.shape() >= 0 && payload.shape() < shapes.length) {
                            shape(player, shapes[payload.shape()]);
                        }
                    }));
        }
    }
}

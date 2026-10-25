package io.github.ChanesCMD.trailcraft.calendar;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.ClientInventoryEvents;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Заметки к дням календаря (ТЗ 3.4): хранятся на сервере, у каждого игрока свои, по ключу «год/сезон/день»
 * (сезон — номер 0..3). Клиент просит все заметки, когда открывает календарь, и отправляет каждую изменённую.
 */
public final class CalendarNotes {
    public static final int MAX_LENGTH = 100, MAX_NOTES = 1000;
    private static final Pattern KEY = Pattern.compile("\\d{1,12}/[0-3]/\\d{1,2}");

    public static String key(Calendar calendar, int day) {
        return calendar.year() + "/" + calendar.season().ordinal() + "/" + day;
    }

    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "calendar_notes_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Сервер → клиент: все заметки игрока. */
    public record Notes(Map<String, String> notes) implements CustomPacketPayload {
        public static final Type<Notes> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "calendar_notes"));
        public static final StreamCodec<ByteBuf, Notes> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(
                HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8).map(Notes::new, Notes::notes);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Клиент → сервер: новая заметка к дню (пустая — удалить). */
    public record Update(String key, String text) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "calendar_note"));
        public static final StreamCodec<ByteBuf, Update> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Update::key,
                ByteBufCodecs.stringUtf8(MAX_LENGTH * 2), Update::text,
                Update::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void update(ServerPlayer player, Update update) {
        if (!KEY.matcher(update.key()).matches()) {
            return;
        }
        String text = update.text().strip();
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        Map<String, String> notes = new HashMap<>(player.getData(ModAttachments.CALENDAR_NOTES));
        if (text.isEmpty()) {
            notes.remove(update.key());
        } else if (notes.size() < MAX_NOTES || notes.containsKey(update.key())) {
            notes.put(update.key(), text);
        }
        player.setData(ModAttachments.CALENDAR_NOTES, notes);
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(Request.TYPE, Request.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    PacketDistributor.sendToPlayer(player, new Notes(player.getData(ModAttachments.CALENDAR_NOTES)));
                }
            }));
            registrar.playToServer(Update.TYPE, Update.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    update(player, payload);
                }
            }));
            registrar.playToClient(Notes.TYPE, Notes.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> ClientInventoryEvents.showCalendarNotes(payload.notes())));
        }
    }

    private CalendarNotes() {}
}

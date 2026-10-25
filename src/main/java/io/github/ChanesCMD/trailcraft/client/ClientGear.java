package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.inventory.GearPayload;

/** Что лежит в надетом рюкзаке (присылает сервер, см. GearPayload): атлас — миникарта и карта, календарь — дата. */
public final class ClientGear {
    private static int flags;

    public static void set(GearPayload payload) {
        flags = payload.flags();
    }

    public static boolean hasAtlas() {
        return (flags & GearPayload.ATLAS) != 0;
    }

    public static boolean hasCalendar() {
        return (flags & GearPayload.CALENDAR) != 0;
    }

    private ClientGear() {}
}

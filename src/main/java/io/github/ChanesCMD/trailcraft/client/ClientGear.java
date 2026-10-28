package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.inventory.GearPayload;

/**
 * Что лежит в надетом рюкзаке и на поясе (присылает сервер, см. GearPayload): атлас — миникарта и карта,
 * календарь — дата, приборы на поясе — точность координат (Coords).
 */
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

    public static boolean hasAltimeter() {
        return (flags & GearPayload.ALTIMETER) != 0;
    }

    public static boolean hasSextant() {
        return (flags & GearPayload.SEXTANT) != 0;
    }

    public static boolean hasChronometer() {
        return (flags & GearPayload.CHRONOMETER) != 0;
    }

    public static boolean hasGps() {
        return (flags & GearPayload.GPS) != 0;
    }

    /** На поясе не полная фляга. */
    public static boolean flaskNeedsWater() {
        return (flags & GearPayload.FLASK) != 0;
    }

    private ClientGear() {}
}

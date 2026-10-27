package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.inventory.GearPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Что лежит в надетом рюкзаке и на поясе (присылает сервер, см. GearPayload): атлас — миникарта и карта,
 * календарь — дата, приборы на поясе — точность координат (Coords).
 */
public final class ClientGear {
    private static int flags;
    /** Кошелёк, пояс и рюкзак (присылает сервер креативному игроку, см. EquippedPayloads). */
    private static final ItemStack[] equipped = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

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

    public static void setEquipped(List<ItemStack> stacks) {
        for (int i = 0; i < equipped.length; i++) {
            equipped[i] = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
        }
    }

    /** Предмет в слоте EquippedPayloads.SLOTS[index]. */
    public static ItemStack equipped(int index) {
        return equipped[index];
    }

    /** Креатив сам поменял слот: показываем сразу, не дожидаясь ответа сервера. */
    public static void setEquipped(int index, ItemStack stack) {
        equipped[index] = stack;
    }

    /** На поясе не полная фляга. */
    public static boolean flaskNeedsWater() {
        return (flags & GearPayload.FLASK) != 0;
    }

    private ClientGear() {}
}

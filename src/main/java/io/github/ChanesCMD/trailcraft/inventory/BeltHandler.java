package io.github.ChanesCMD.trailcraft.inventory;

import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModItems;
import io.github.ChanesCMD.trailcraft.item.FlaskItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Содержимое надетого пояса: фляга с питьевой водой и три слота приборов — высотомер, секстант, хронометр
 * (подсумки и связь — позже). В любой слот прибора можно положить GPS, тогда два других закрыты.
 */
public class BeltHandler extends ContentsHandler {
    public static final int FLASK = 0;
    public static final int ALTIMETER = 1, SEXTANT = 2, CHRONOMETER = 3;
    public static final int INSTRUMENTS = ALTIMETER, INSTRUMENT_COUNT = 3;
    public static final int SIZE = 4;

    public BeltHandler(Player player) {
        super(player, SIZE);
    }

    /** Общий экземпляр игрока (сам замечает, что пояс сменился), чтобы не создавать новый каждый тик. */
    public static BeltHandler of(Player player) {
        return player.getData(ModAttachments.BELT_HANDLER);
    }

    @Override
    protected ItemStack holder() {
        return player.getData(ModAttachments.PLAYER_SLOTS).getStackInSlot(PlayerSlots.BELT);
    }

    /** Фляга на поясе или пустой стак. */
    public ItemStack flask() {
        ItemStack stack = getStackInSlot(FLASK);
        return stack.getItem() instanceof FlaskItem ? stack : ItemStack.EMPTY;
    }

    /** Меняет запас воды во фляге на поясе. */
    public void setFlaskWater(int water) {
        ItemStack flask = flask();
        if (!flask.isEmpty()) {
            ItemStack copy = flask.copy();
            FlaskItem.setWater(copy, water);
            setStackInSlot(FLASK, copy);
        }
    }

    /** Прибор, для которого сделан слот (кроме него слот принимает только GPS). */
    public static Item instrumentFor(int slot) {
        return switch (slot) {
            case ALTIMETER -> ModItems.BRASS_ALTIMETER.get();
            case SEXTANT -> ModItems.SEXTANT.get();
            default -> ModItems.CHRONOMETER.get();
        };
    }

    public static boolean isInstrumentSlot(int slot) {
        return slot >= INSTRUMENTS && slot < INSTRUMENTS + INSTRUMENT_COUNT;
    }

    /** Слот прибора, где лежит GPS, или -1. Если GPS несколько, считается первый. */
    public int gpsSlot() {
        for (int i = INSTRUMENTS; i < INSTRUMENTS + INSTRUMENT_COUNT; i++) {
            if (getStackInSlot(i).is(ModItems.GPS.get())) {
                return i;
            }
        }
        return -1;
    }

    /** Слот прибора закрыт: GPS лежит в другом слоте. */
    public boolean isBlocked(int slot) {
        int gps = gpsSlot();
        return gps >= 0 && gps != slot;
    }

    public boolean hasGps() {
        return gpsSlot() >= 0;
    }

    /** На поясе лежит свой прибор этого слота (и GPS его не закрыл). */
    public boolean hasInstrument(int slot) {
        return !isBlocked(slot) && getStackInSlot(slot).is(instrumentFor(slot));
    }

    /** Наполняет флягу на поясе доверху водой этого вида, если можно (FlaskItem.canFill). */
    public void fillFlask(boolean salty) {
        ItemStack flask = flask();
        if (!flask.isEmpty() && FlaskItem.canFill(flask, salty)) {
            ItemStack copy = flask.copy();
            FlaskItem.fill(copy, salty);
            setStackInSlot(FLASK, copy);
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!isPresent()) {
            return false;
        }
        if (isInstrumentSlot(slot)) {
            return !isBlocked(slot) && (stack.is(instrumentFor(slot)) || stack.is(ModItems.GPS.get()));
        }
        return stack.getItem() instanceof FlaskItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
}

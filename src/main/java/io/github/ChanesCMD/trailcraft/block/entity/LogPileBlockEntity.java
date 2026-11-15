package io.github.ChanesCMD.trailcraft.block.entity;

import io.github.ChanesCMD.trailcraft.init.ModBlockEntities;
import io.github.ChanesCMD.trailcraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Поленница (LogPileBlock), как в TFC: 16 слотов сечением 4×4, в каждом брёвна одного вида — до 4 толстых или до 16 тонких.
 * Слоты заполняются слева направо, а потом снизу вверх (слот 0 — левый нижний, 3 — правый нижний, 4 — левый во втором
 * ряду…). Бревно берётся из слота, на который смотрит игрок (slotAt, nearestFilled), а брёвна над освободившимся слотом
 * опускаются вниз ({@link #settle}). Сетка занимает середину блока шириной 12 пикселей, по бокам стоят палки-подпорки
 * (client/LogPileRenderer), клетка — 3×3 пикселя.
 */
public class LogPileBlockEntity extends BlockEntity {
    public static final int COLUMNS = 4, ROWS = 4, SLOTS = COLUMNS * ROWS;
    /** Клетка слота и отступ сетки от края блока (по 2 пикселя слева и справа под палки-подпорки), в долях блока. */
    public static final float CELL = 3 / 16f, MARGIN = 2 / 16f;

    /** Сколько брёвен в слоте: толстых 4, тонких 16 (всего в поленнице 64 толстых или 256 тонких). */
    public static final int THICK_CAPACITY = 4, THIN_CAPACITY = 16;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public LogPileBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOG_PILE.get(), pos, state);
    }

    /** Сколько таких брёвен влезает в слот: 16 тонких, 4 толстых (#minecraft:logs), 0 — не бревно. */
    public static int capacity(ItemStack stack) {
        if (stack.is(ModTags.THIN_LOGS)) {
            return THIN_CAPACITY;
        }
        return stack.is(ItemTags.LOGS) ? THICK_CAPACITY : 0;
    }

    /**
     * Слот под курсором: local — точка попадания внутри блока (0..1 по осям), axis — вдоль какой оси лежат брёвна.
     * Колонки идут поперёк брёвен (при оси Z — по X, при оси X — по Z, в обратную сторону: так их рисует LogPileRenderer),
     * ряды — снизу вверх.
     */
    public static int slotAt(Vec3 local, Direction.Axis axis) {
        double across = axis == Direction.Axis.Z ? local.x : 1 - local.z;
        int column = Mth.clamp((int) Math.floor((across - MARGIN) / CELL), 0, COLUMNS - 1);
        int row = Mth.clamp((int) Math.floor(local.y / CELL), 0, ROWS - 1);
        return row * COLUMNS + column;
    }

    /** Ближайший к slot непустой слот (по сетке), -1 — поленница пуста. */
    public int nearestFilled(int slot) {
        int best = -1, bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                continue;
            }
            int distance = Math.abs(i % COLUMNS - slot % COLUMNS) + Math.abs(i / COLUMNS - slot / COLUMNS);
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && capacity(stack) > 0;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    /** Сколько брёвен из stack (не больше max) ещё влезет: сначала в слоты с таким же бревном, потом в пустые. */
    public int room(ItemStack stack, int max) {
        return place(stack, max, false);
    }

    /** Кладёт до max брёвен из stack (сам stack не меняет); возвращает, сколько легло. */
    public int insert(ItemStack stack, int max) {
        int moved = place(stack, max, true);
        if (moved > 0) {
            changed();
        }
        return moved;
    }

    private int place(ItemStack stack, int max, boolean apply) {
        int capacity = capacity(stack);
        int moved = 0;
        for (int pass = 0; pass < 2 && moved < max; pass++) {
            for (int i = 0; i < SLOTS && moved < max; i++) {
                ItemStack slot = items.get(i);
                boolean fits = pass == 0 ? !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack) : slot.isEmpty();
                if (!fits) {
                    continue;
                }
                int count = Math.min(capacity - slot.getCount(), max - moved);
                if (count <= 0) {
                    continue;
                }
                if (apply) {
                    if (slot.isEmpty()) {
                        items.set(i, stack.copyWithCount(count));
                    } else {
                        slot.grow(count);
                    }
                }
                moved += count;
            }
        }
        return moved;
    }

    /** Достаёт до max брёвен из слота. */
    public ItemStack take(int slotIndex, int max) {
        ItemStack slot = items.get(slotIndex);
        if (slot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = slot.split(Math.min(max, slot.getCount()));
        settle();
        changed();
        return out;
    }

    /** Брёвна над освободившимся слотом опускаются вниз: в каждой колонке пустые слоты уходят наверх. */
    private void settle() {
        for (int column = 0; column < COLUMNS; column++) {
            int free = 0;
            for (int row = 0; row < ROWS; row++) {
                int slot = row * COLUMNS + column;
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (row != free) {
                    items.set(free * COLUMNS + column, stack);
                    items.set(slot, ItemStack.EMPTY);
                }
                free++;
            }
        }
    }

    /** Самый высокий ряд, в котором есть брёвна; -1 — поленница пуста. */
    public int topRow() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!items.get(slot).isEmpty()) {
                return slot / COLUMNS;
            }
        }
        return -1;
    }

    public void dropAll(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, items);
        items.clear();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

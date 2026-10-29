package io.github.ChanesCMD.trailcraft.bee;

import io.github.ChanesCMD.trailcraft.init.ModBees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Пасека: две пчелы (слоты 0, 1) и 9 слотов выхода. Днём, без дождя и под открытым небом каждые BeeSpecies.cycle тиков
 * (у двух разных — по более быстрой) каждая пчела даёт свои соты; если пчёл две — с шансом MUTATION_CHANCE появляется
 * вид, который из них выводится (BeeSpecies.mutation), а если такого нет — с шансом OFFSPRING_CHANCE пчела вида одного
 * из родителей. Пчёлы не тратятся. Пока выход полон, пасека стоит. Воронки и желоба кладут пчёл и забирают выход.
 */
public class ApiaryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BEE_A = 0, BEE_B = 1, OUTPUT = 2, SLOTS = 11;
    /** Почему пасека стоит (для окна): работает, нет пчёл, ночь, дождь, нет неба, выход полон. */
    public static final int OK = 0, NO_BEES = 1, NIGHT = 2, RAIN = 3, NO_SKY = 4, FULL = 5;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < OUTPUT && stack.getItem() instanceof BeeItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < OUTPUT ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Для воронок и желобов Create: пчёл — только положить, выход — только забрать. */
    public final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < OUTPUT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < OUTPUT ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < OUTPUT && items.isItemValid(slot, stack);
        }
    };
    private int progress, cycle, status = NO_BEES;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> cycle;
                default -> status;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> cycle = value;
                default -> status = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ApiaryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBees.APIARY_ENTITY.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    @Nullable
    private BeeSpecies bee(int slot) {
        return items.getStackInSlot(slot).getItem() instanceof BeeItem bee ? bee.species : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ApiaryBlockEntity be) {
        be.tick(level, pos);
    }

    private void tick(Level level, BlockPos pos) {
        BeeSpecies a = bee(BEE_A), b = bee(BEE_B);
        int before = status;
        if (a == null && b == null) {
            status = NO_BEES;
        } else if (!level.isDay()) {
            status = NIGHT;
        } else if (level.isRainingAt(pos.above())) {
            status = RAIN;
        } else if (!level.canSeeSky(pos.above())) {
            status = NO_SKY;
        } else {
            status = OK;
        }
        cycle = a == null ? b == null ? 0 : b.cycle : b == null ? a.cycle : Math.min(a.cycle, b.cycle);
        if (status == OK && ++progress >= cycle) {
            List<ItemStack> products = produce(level, a, b);
            if (fits(products)) {
                products.forEach(this::output);
                progress = 0;
            } else {
                status = FULL;
                progress = cycle;
            }
        }
        if (status == OK || before != status) {
            setChanged();
        }
    }

    private List<ItemStack> produce(Level level, @Nullable BeeSpecies a, @Nullable BeeSpecies b) {
        List<ItemStack> out = new ArrayList<>();
        for (BeeSpecies s : new BeeSpecies[]{a, b}) {
            if (s != null) {
                out.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("trailcraft", s.comb)), s.combs));
            }
        }
        if (a != null && b != null) {
            BeeSpecies child = a == b ? null : BeeSpecies.mutation(a, b);
            float roll = level.random.nextFloat();
            if (child != null && roll < BeeSpecies.MUTATION_CHANCE) {
                out.add(new ItemStack(ModBees.BEES.get(child).get()));
            } else if (child == null && roll < BeeSpecies.OFFSPRING_CHANCE) {
                out.add(new ItemStack(ModBees.BEES.get(level.random.nextBoolean() ? a : b).get()));
            }
        }
        return out;
    }

    /** Влезет ли всё в выход (на копии слотов). */
    private boolean fits(List<ItemStack> products) {
        ItemStackHandler copy = new ItemStackHandler(SLOTS - OUTPUT);
        for (int i = OUTPUT; i < SLOTS; i++) {
            copy.setStackInSlot(i - OUTPUT, items.getStackInSlot(i).copy());
        }
        for (ItemStack product : products) {
            ItemStack rest = product.copy();
            for (int i = 0; i < copy.getSlots() && !rest.isEmpty(); i++) {
                rest = copy.insertItem(i, rest, false);
            }
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void output(ItemStack product) {
        ItemStack rest = product.copy();
        for (int i = OUTPUT; i < SLOTS && !rest.isEmpty(); i++) {
            ItemStack in = items.getStackInSlot(i);
            if (in.isEmpty()) {
                items.setStackInSlot(i, rest);
                return;
            }
            if (ItemStack.isSameItemSameComponents(in, rest) && in.getCount() < in.getMaxStackSize()) {
                int move = Math.min(rest.getCount(), in.getMaxStackSize() - in.getCount());
                items.setStackInSlot(i, in.copyWithCount(in.getCount() + move));
                rest.shrink(move);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        progress = tag.getInt("progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.trailcraft.apiary");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ApiaryMenu(id, inventory, items, data);
    }
}

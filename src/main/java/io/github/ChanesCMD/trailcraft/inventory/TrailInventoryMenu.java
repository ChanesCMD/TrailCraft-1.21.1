package io.github.ChanesCMD.trailcraft.inventory;

import com.mojang.datafixers.util.Pair;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
import io.github.ChanesCMD.trailcraft.item.BackpackItem;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * Инвентарь игрока (ТЗ, часть 1, разделы 1, 1.3, 3.1). Координаты ниже — левый верхний угол рамки слота 18×18
 * относительно панели 388×172; сам предмет рисуется на 1 px правее и ниже.
 *
 * Блокировка: без рюкзака доступны только хотбар и крафт 2×2. Рюкзак (в своём слоте или в левой руке)
 * открывает слоты основного инвентаря, верстак в рюкзаке — крафт 3×3.
 * Из заблокированного слота предмет можно забрать, положить — нельзя. Рюкзак в хотбар и крафт не кладётся.
 */
public class TrailInventoryMenu extends AbstractContainerMenu {
    // === Разметка панели (общая с экраном) ===
    public static final int WIDTH = 388, HEIGHT = 172;
    public static final int COSMETIC_X = 7, ARMOR_X = 83, WARMTH_X = 63, EQUIP_Y = 35;
    public static final int MODEL_X = 29, MODEL_Y = 35, MODEL_W = 50, MODEL_H = 72;
    public static final int ACCESSORY_X = 29, ACCESSORY_Y = 109;
    public static final int BOTTOM_Y = 147, WALLET_X = 29, BELT_X = 47, BACKPACK_X = 65, OFFHAND_X = 83;
    public static final int MAIN_X = 107, MAIN_Y = 35, HOTBAR_Y = 147;
    public static final int CRAFT_X = 279, CRAFT_Y = 35, RESULT_X = 363, RESULT_Y = 53;
    /** Окно рюкзака 126×201 над слотом рюкзака: левый край = левый край слота, низ на 2 px выше слота. */
    public static final int BACKPACK_W = 126, BACKPACK_H = 201;
    public static final int BACKPACK_WIN_X = BACKPACK_X, BACKPACK_WIN_Y = BOTTOM_Y - 2 - BACKPACK_H;

    // === Номера слотов меню ===
    public static final int RESULT = 0;
    public static final int CRAFT_START = 1;          // 9
    public static final int ARMOR_START = 10;         // 4: шлем … ботинки
    public static final int COSMETIC_START = 14;      // 4
    public static final int WARMTH_START = 18;        // 4
    public static final int ACCESSORY_START = 22;     // 8
    public static final int WALLET_SLOT = 30, BELT_SLOT = 31, BACKPACK_SLOT = 32, OFFHAND_SLOT = 33;
    public static final int MAIN_START = 34;          // 54: содержимое рюкзака
    public static final int HOTBAR_START = 88;        // 9
    public static final int BACKPACK_UTILITY_START = 97;  // 7: аккумулятор … календарь
    public static final int POCKET_START = 104;       // 6
    public static final int SLOT_COUNT = 110;

    /** Группы слотов: от группы зависит, когда слот виден. */
    public static final int GROUP_BASE = 0, GROUP_WARMTH = 1, GROUP_BACKPACK = 2;

    /** Открытое окно поверх инвентаря (только на клиенте). */
    public enum Popup { NONE, BACKPACK, DEATHS }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final Player player;
    private final boolean clientSide;
    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final BackpackHandler backpack;

    // === Состояние окна на клиенте (экран меняет его, слоты по нему решают, видны ли они) ===
    public boolean warmthVisible;
    public Popup popup = Popup.NONE;
    public int popupX, popupY, popupW, popupH;

    public TrailInventoryMenu(int containerId, Inventory inventory) {
        super(ModMenus.INVENTORY.get(), containerId);
        this.player = inventory.player;
        this.clientSide = player.level().isClientSide;
        this.backpack = new BackpackHandler(player);
        PlayerSlots extra = player.getData(ModAttachments.PLAYER_SLOTS);
        IItemHandler vanilla = new InvWrapper(inventory);
        IItemHandler craft = new InvWrapper(craftSlots);

        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, RESULT_X + 1, RESULT_Y + 1) {
            @Override
            public boolean isActive() {
                return !coveredByPopup(this);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boolean outer = row == 2 || col == 2;
                addSlot(new MenuSlot(craft, col + row * 3, CRAFT_X + 1 + col * 18, CRAFT_Y + 1 + row * 18)
                        .locked(outer ? () -> !hasWorkbench() : null).noBackpack());
            }
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new MenuSlot(vanilla, 39 - i, ARMOR_X + 1, EQUIP_Y + 1 + i * 18).equipment(ARMOR[i], ARMOR_ICONS[i]));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new MenuSlot(extra, PlayerSlots.COSMETIC + i, COSMETIC_X + 1, EQUIP_Y + 1 + i * 18).equipment(ARMOR[i], ARMOR_ICONS[i]).cosmetic());
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new MenuSlot(extra, PlayerSlots.WARMTH + i, WARMTH_X + 1, EQUIP_Y + 1 + i * 18).group(GROUP_WARMTH));
        }
        for (int i = 0; i < 8; i++) {
            addSlot(new MenuSlot(extra, PlayerSlots.ACCESSORIES + i, ACCESSORY_X + 1 + (i % 4) * 18, ACCESSORY_Y + 1 + (i / 4) * 18));
        }
        addSlot(new MenuSlot(extra, PlayerSlots.WALLET, WALLET_X + 1, BOTTOM_Y + 1));
        addSlot(new MenuSlot(extra, PlayerSlots.BELT, BELT_X + 1, BOTTOM_Y + 1));
        addSlot(new MenuSlot(extra, PlayerSlots.BACKPACK, BACKPACK_X + 1, BOTTOM_Y + 1));
        addSlot(new MenuSlot(vanilla, Inventory.SLOT_OFFHAND, OFFHAND_X + 1, BOTTOM_Y + 1)
                .equipment(EquipmentSlot.OFFHAND, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
        for (int i = 0; i < BackpackItem.MAIN_SIZE; i++) {
            int index = i;
            addSlot(new MenuSlot(backpack, BackpackItem.MAIN + i, MAIN_X + 1 + (i % 9) * 18, MAIN_Y + 1 + (i / 9) * 18)
                    .locked(() -> index >= backpack.capacity()));
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new MenuSlot(vanilla, col, MAIN_X + 1 + col * 18, HOTBAR_Y + 1).noBackpack());
        }
        for (int i = 0; i < 7; i++) {
            addSlot(new MenuSlot(backpack, BackpackItem.BATTERY + i, BACKPACK_WIN_X + 8, BACKPACK_WIN_Y + 21 + 21 * i)
                    .group(GROUP_BACKPACK).locked(() -> !backpack.isPresent()));
        }
        for (int k = 0; k < 6; k++) {
            addSlot(new MenuSlot(backpack, BackpackItem.POCKETS + k, BACKPACK_WIN_X + 8 + 18 * k, BACKPACK_WIN_Y + 177)
                    .group(GROUP_BACKPACK).locked(() -> !backpack.isPresent()));
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return player.getData(ModAttachments.COSMETIC_TOGGLES);
            }

            @Override
            public void set(int value) {
                player.setData(ModAttachments.COSMETIC_TOGGLES, value);
            }
        });
    }

    public BackpackHandler backpack() {
        return backpack;
    }

    public boolean hasWorkbench() {
        return backpack.has(BackpackItem.WORKBENCH);
    }

    public boolean hasCalendar() {
        return backpack.has(BackpackItem.CALENDAR);
    }

    public boolean isCosmeticShown(int armorIndex) {
        return (player.getData(ModAttachments.COSMETIC_TOGGLES) & (1 << armorIndex)) != 0;
    }

    /** Слот лежит под открытым окном и потому не виден (окна на клиенте). */
    private boolean coveredByPopup(Slot slot) {
        return popup != Popup.NONE && slot.x - 1 < popupX + popupW && slot.x + 17 > popupX
                && slot.y - 1 < popupY + popupH && slot.y + 17 > popupY;
    }

    /** Кнопки 0–3 — переключатели косметики (клиент шлёт через handleInventoryButtonClick). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 4) {
            player.setData(ModAttachments.COSMETIC_TOGGLES, player.getData(ModAttachments.COSMETIC_TOGGLES) ^ (1 << id));
            return true;
        }
        return false;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots && player instanceof ServerPlayer serverPlayer) {
            CraftingInput input = craftSlots.asCraftInput();
            ItemStack result = ItemStack.EMPTY;
            var recipe = serverPlayer.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, serverPlayer.level());
            if (recipe.isPresent() && !hasItemsInLockedCraftSlots()
                    && resultSlots.setRecipeUsed(serverPlayer.level(), serverPlayer, recipe.get())) {
                ItemStack assembled = recipe.get().value().assemble(input, serverPlayer.level().registryAccess());
                if (assembled.isItemEnabled(serverPlayer.level().enabledFeatures())) {
                    result = assembled;
                }
            }
            resultSlots.setItem(0, result);
            setRemoteSlot(RESULT, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, result));
        }
    }

    /** Верстак убрали, а во внешнем ряду крафта остались предметы — рецепт не считается, пока их не заберут. */
    private boolean hasItemsInLockedCraftSlots() {
        for (int i = CRAFT_START; i < CRAFT_START + 9; i++) {
            if (slots.get(i) instanceof MenuSlot slot && slot.isLocked() && slot.hasItem()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        resultSlots.clearContent();
        if (!player.level().isClientSide) {
            clearContainer(player, craftSlots);
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int inventoryEnd = HOTBAR_START + 9;

        if (index == RESULT) {
            stack.getItem().onCraftedBy(stack, player.level(), player);
            if (!moveItemStackTo(stack, MAIN_START, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= MAIN_START && index < inventoryEnd) {
            int armor = switch (player.getEquipmentSlotForItem(copy)) {
                case HEAD -> ARMOR_START;
                case CHEST -> ARMOR_START + 1;
                case LEGS -> ARMOR_START + 2;
                case FEET -> ARMOR_START + 3;
                default -> -1;
            };
            if (armor >= 0 && !slots.get(armor).hasItem()) {
                if (!moveItemStackTo(stack, armor, armor + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MAIN_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, MAIN_START, inventoryEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == RESULT) {
            player.drop(stack, false);
        }
        return copy;
    }

    /**
     * Слот меню поверх любого IItemHandler: ванильного инвентаря, PlayerSlots, рюкзака, сетки крафта.
     * Умеет блокировку, видимость по группе, ограничения брони.
     */
    public class MenuSlot extends SlotItemHandler {
        private int group = GROUP_BASE;
        @Nullable
        private BooleanSupplier locked;
        @Nullable
        private EquipmentSlot equipment;
        @Nullable
        private ResourceLocation icon;
        private boolean cosmetic;
        private boolean noBackpack;

        MenuSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        MenuSlot group(int group) {
            this.group = group;
            return this;
        }

        MenuSlot locked(@Nullable BooleanSupplier locked) {
            this.locked = locked;
            return this;
        }

        MenuSlot equipment(EquipmentSlot equipment, ResourceLocation icon) {
            this.equipment = equipment;
            this.icon = icon;
            return this;
        }

        /** Косметика: тот же тип брони, но без проклятий и звука надевания. */
        MenuSlot cosmetic() {
            this.cosmetic = true;
            return this;
        }

        /** Рюкзак носится только в своём слоте или в левой руке. */
        MenuSlot noBackpack() {
            this.noBackpack = true;
            return this;
        }

        public int group() {
            return group;
        }

        public boolean isLocked() {
            return locked != null && locked.getAsBoolean();
        }

        @Override
        public boolean isActive() {
            if (!clientSide) return true;
            return switch (group) {
                case GROUP_WARMTH -> warmthVisible && !coveredByPopup(this);
                case GROUP_BACKPACK -> popup == Popup.BACKPACK;
                default -> !coveredByPopup(this);
            };
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (isLocked() || noBackpack && stack.getItem() instanceof BackpackItem) return false;
            boolean armor = equipment != null && equipment != EquipmentSlot.OFFHAND;
            if (armor && player.getEquipmentSlotForItem(stack) != equipment) return false;
            return super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            if (equipment != null && !cosmetic && !stack.isEmpty() && !player.isCreative()
                    && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return false;
            }
            return super.mayPickup(player);
        }

        @Override
        public int getMaxStackSize() {
            return equipment != null && equipment != EquipmentSlot.OFFHAND ? 1 : super.getMaxStackSize();
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            if (equipment != null && !cosmetic) {
                player.onEquipItem(equipment, oldStack, newStack);
            }
            super.setByPlayer(newStack, oldStack);
        }

        /** Предмет в слоте могли изменить напрямую (grow/shrink) — записываем его обратно в хранилище. */
        @Override
        public void setChanged() {
            if (getItemHandler() instanceof IItemHandlerModifiable handler) {
                handler.setStackInSlot(getContainerSlot(), getItem());
            }
            super.setChanged();
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return icon != null ? Pair.of(InventoryMenu.BLOCK_ATLAS, icon) : super.getNoItemIcon();
        }
    }
}

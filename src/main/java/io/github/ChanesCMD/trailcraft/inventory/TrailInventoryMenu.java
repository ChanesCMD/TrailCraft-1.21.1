package io.github.ChanesCMD.trailcraft.inventory;

import com.mojang.datafixers.util.Pair;
import io.github.ChanesCMD.trailcraft.init.ModAttachments;
import io.github.ChanesCMD.trailcraft.init.ModMenus;
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
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Инвентарь игрока (ТЗ, часть 1, раздел 1). Координаты ниже — левый верхний угол рамки слота 18×18
 * относительно панели 388×172; сам предмет рисуется на 1 px правее и ниже.
 * Блокировки слотов пока нет (шаг 2).
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

    // === Номера слотов меню ===
    public static final int RESULT = 0;
    public static final int CRAFT_START = 1;          // 9
    public static final int ARMOR_START = 10;         // 4: шлем … ботинки
    public static final int COSMETIC_START = 14;      // 4
    public static final int WARMTH_START = 18;        // 4
    public static final int ACCESSORY_START = 22;     // 8
    public static final int WALLET_SLOT = 30, BELT_SLOT = 31, BACKPACK_SLOT = 32, OFFHAND_SLOT = 33;
    public static final int MAIN_START = 34;          // 54: ряды 1–3 ванильные, 4–6 из PlayerSlots
    public static final int HOTBAR_START = 88;        // 9
    public static final int SLOT_COUNT = 97;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final Player player;
    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();

    /** Столбик «Тепло» виден только при наведении на броню; на сервере слоты всегда активны. */
    public boolean warmthVisible;

    public TrailInventoryMenu(int containerId, Inventory inventory) {
        super(ModMenus.INVENTORY.get(), containerId);
        this.player = inventory.player;
        this.warmthVisible = !player.level().isClientSide;
        PlayerSlots extra = player.getData(ModAttachments.PLAYER_SLOTS);

        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, RESULT_X + 1, RESULT_Y + 1));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, CRAFT_X + 1 + col * 18, CRAFT_Y + 1 + row * 18));
            }
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new ArmorSlot(inventory, 39 - i, ARMOR_X + 1, EQUIP_Y + 1 + i * 18, ARMOR[i], ARMOR_ICONS[i]));
        }
        for (int i = 0; i < 4; i++) {
            EquipmentSlot type = ARMOR[i];
            ResourceLocation icon = ARMOR_ICONS[i];
            addSlot(new SlotItemHandler(extra, PlayerSlots.COSMETIC + i, COSMETIC_X + 1, EQUIP_Y + 1 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return player.getEquipmentSlotForItem(stack) == type && super.mayPlace(stack);
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
                }
            });
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new SlotItemHandler(extra, PlayerSlots.WARMTH + i, WARMTH_X + 1, EQUIP_Y + 1 + i * 18) {
                @Override
                public boolean isActive() {
                    return warmthVisible;
                }
            });
        }
        for (int i = 0; i < 8; i++) {
            addSlot(new SlotItemHandler(extra, PlayerSlots.ACCESSORIES + i, ACCESSORY_X + 1 + (i % 4) * 18, ACCESSORY_Y + 1 + (i / 4) * 18));
        }
        addSlot(new SlotItemHandler(extra, PlayerSlots.WALLET, WALLET_X + 1, BOTTOM_Y + 1));
        addSlot(new SlotItemHandler(extra, PlayerSlots.BELT, BELT_X + 1, BOTTOM_Y + 1));
        addSlot(new SlotItemHandler(extra, PlayerSlots.BACKPACK, BACKPACK_X + 1, BOTTOM_Y + 1));
        addSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, OFFHAND_X + 1, BOTTOM_Y + 1) {
            @Override
            public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                player.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.setByPlayer(newStack, oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int x = MAIN_X + 1 + col * 18, y = MAIN_Y + 1 + row * 18;
                if (row < 3) {
                    addSlot(new Slot(inventory, 9 + col + row * 9, x, y));
                } else {
                    addSlot(new SlotItemHandler(extra, PlayerSlots.EXTRA_MAIN + col + (row - 3) * 9, x, y));
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, MAIN_X + 1 + col * 18, HOTBAR_Y + 1));
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

    public boolean isCosmeticShown(int armorIndex) {
        return (player.getData(ModAttachments.COSMETIC_TOGGLES) & (1 << armorIndex)) != 0;
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
            if (recipe.isPresent() && resultSlots.setRecipeUsed(serverPlayer.level(), serverPlayer, recipe.get())) {
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

        if (index == RESULT) {
            stack.getItem().onCraftedBy(stack, player.level(), player);
            if (!moveItemStackTo(stack, MAIN_START, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= MAIN_START) {
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
                if (!moveItemStackTo(stack, HOTBAR_START, SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MAIN_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, MAIN_START, SLOT_COUNT, false)) {
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

    /** Слот брони как в ванилле: только свой тип, проклятие несъёмности, звук надевания. */
    private static class ArmorSlot extends Slot {
        private final Player owner;
        private final EquipmentSlot type;
        private final ResourceLocation icon;

        ArmorSlot(Inventory inventory, int index, int x, int y, EquipmentSlot type, ResourceLocation icon) {
            super(inventory, index, x, y);
            this.owner = inventory.player;
            this.type = type;
            this.icon = icon;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            owner.onEquipItem(type, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return owner.getEquipmentSlotForItem(stack) == type;
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                    && super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
        }
    }
}

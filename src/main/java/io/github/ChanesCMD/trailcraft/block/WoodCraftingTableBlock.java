package io.github.ChanesCMD.trailcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Верстак из досок своей породы (4 малые доски). Работает как ванильный: то же окно крафта 3×3. Ванильное окно
 * закрывается, если блок — не minecraft:crafting_table, поэтому своё меню проверяет, что игрок стоит у верстака TrailCraft.
 */
public class WoodCraftingTableBlock extends CraftingTableBlock {
    public static final MapCodec<WoodCraftingTableBlock> CODEC = simpleCodec(WoodCraftingTableBlock::new);
    private static final Component TITLE = Component.translatable("container.crafting");

    public WoodCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new Menu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }

    /** Ванильное окно крафта, которое не закрывается у верстака TrailCraft. */
    public static class Menu extends CraftingMenu {
        private final ContainerLevelAccess access;

        public Menu(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
            this.access = access;
        }

        @Override
        public boolean stillValid(Player player) {
            return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof WoodCraftingTableBlock
                    && player.canInteractWithBlock(pos, 4.0), true);
        }
    }
}

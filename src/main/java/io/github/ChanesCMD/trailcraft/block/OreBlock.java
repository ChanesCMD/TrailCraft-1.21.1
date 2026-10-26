package io.github.ChanesCMD.trailcraft.block;

import io.github.ChanesCMD.trailcraft.ore.Ore;
import io.github.ChanesCMD.trailcraft.ore.Rock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Руда в породе: один блок на руду, порода — свойство {@code rock} со своим набором значений у каждой руды.
 * Порода хранится и в предмете (компонент block_state): так её сохраняют шёлковое касание и выбор блока колёсиком.
 */
public class OreBlock extends Block {
    /** Свойство нужно уже в конструкторе Block (createBlockStateDefinition), до того как заполнены поля. */
    private static final ThreadLocal<Ore> PENDING = new ThreadLocal<>();

    public final Ore ore;

    public static OreBlock create(Ore ore, BlockBehaviour.Properties properties) {
        PENDING.set(ore);
        try {
            return new OreBlock(ore, properties);
        } finally {
            PENDING.remove();
        }
    }

    private OreBlock(Ore ore, BlockBehaviour.Properties properties) {
        super(properties);
        this.ore = ore;
        registerDefaultState(stateDefinition.any().setValue(ore.rockProperty, ore.rocks.get(0)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PENDING.get().rockProperty);
    }

    public Rock rock(BlockState state) {
        return state.getValue(ore.rockProperty);
    }

    /** Предмет руды в нужной породе. */
    public ItemStack stack(Rock rock) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(ore.rockProperty, rock));
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stack(rock(state));
    }
}

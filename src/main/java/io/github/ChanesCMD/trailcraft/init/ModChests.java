package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.block.WoodChestBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сундуки по породам древесины (8 досок породы — сундук): <порода>_chest, кроме вариантов листвы (красный клён, яблоня —
 * у них доски клёна и дуба). JSON и текстуры — scripts/gen_chests.py.
 */
public final class ModChests {
    public static final Map<String, DeferredBlock<WoodChestBlock>> CHESTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEMS = new LinkedHashMap<>();

    static {
        for (ModWood.WoodSet set : ModWood.SETS) {
            if (set.smallBoard == null) {
                continue;
            }
            String id = set.id + "_chest";
            DeferredBlock<WoodChestBlock> block = ModBlocks.BLOCKS.registerBlock(id, p -> new WoodChestBlock(set.id, p),
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                            .strength(2.5f).sound(SoundType.WOOD).ignitedByLava());
            CHESTS.put(set.id, block);
            ITEMS.put(set.id, ModItems.ITEMS.registerSimpleBlockItem(id, block));
        }
    }

    /** Вызывается из конструктора мода после ModWood.init(). */
    public static void init() {}

    private ModChests() {}
}

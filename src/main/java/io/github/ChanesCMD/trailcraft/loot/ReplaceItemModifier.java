package io.github.ChanesCMD.trailcraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Во всём луте (мобы, сундуки, рыбалка…) заменяет один предмет другим с тем же числом: ванильные перо и кость — на
 * свои (data/trailcraft/loot_modifiers/feather.json, bone.json; список — data/neoforge/loot_modifiers/global_loot_modifiers.json).
 */
public class ReplaceItemModifier extends LootModifier {
    public static final MapCodec<ReplaceItemModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("from").forGetter(m -> m.from))
            .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(m -> m.to))
            .apply(instance, ReplaceItemModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TrailCraft.MODID);

    static {
        SERIALIZERS.register("replace_item", () -> CODEC);
    }

    private final Item from, to;

    public ReplaceItemModifier(LootItemCondition[] conditions, Item from, Item to) {
        super(conditions);
        this.from = from;
        this.to = to;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        for (int i = 0; i < loot.size(); i++) {
            ItemStack stack = loot.get(i);
            if (stack.is(from)) {
                loot.set(i, stack.transmuteCopy(to, stack.getCount()));
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

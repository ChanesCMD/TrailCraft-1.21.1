package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Монета. Виды по возрастанию, каждый в RATE раз дороже предыдущего (см. ModItems.COINS). */
public class CoinItem extends Item {
    /** Медь, бронза, серебро, золото, платина, драгоценная платина, иридий, драгоценный иридий. */
    public static final int TIERS = 8;
    /** Сколько монет одного вида меняется на одну следующего. */
    public static final int RATE = 10;

    private final int tier;

    public CoinItem(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    /** Сколько медных монет стоит одна такая. */
    public long value() {
        long value = 1;
        for (int i = 0; i < tier; i++) value *= RATE;
        return value;
    }

    public static ItemStack stack(int tier, int count) {
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(ModItems.COINS.get(tier).get(), count);
    }
}

package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

/** Зачарования мода. Сами они описаны в data/trailcraft/enchantment, здесь только ключи для кода. */
public final class ModEnchantments {
    /** «Выпад» (бэкпорт 1.21.11): укол копьём толкает владельца вперёд. Действие — SpearCombat.lungeLevel. */
    public static final ResourceKey<Enchantment> LUNGE =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "lunge"));

    private ModEnchantments() {}
}

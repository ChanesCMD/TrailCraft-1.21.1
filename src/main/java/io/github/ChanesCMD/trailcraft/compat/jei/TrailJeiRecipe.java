package io.github.ChanesCMD.trailcraft.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Рецепт для JEI: до 4 слотов входа (в каждом — варианты по кругу), результат и пояснение под ними. */
public record TrailJeiRecipe(List<List<ItemStack>> inputs, ItemStack output, Component note) {}

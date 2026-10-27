package io.github.ChanesCMD.trailcraft.compat.jei;

import io.github.ChanesCMD.trailcraft.client.gui.UiDraw;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Категория JEI для рецептов TrailCraft, которых нет в сетке крафта: входы → стрелка → результат, ниже пояснение. */
public class TrailJeiCategory implements IRecipeCategory<TrailJeiRecipe> {
    static final int WIDTH = 160, HEIGHT = 50, ARROW_X = 78, OUTPUT_X = 108;

    private final RecipeType<TrailJeiRecipe> type;
    private final Component title;
    private final IDrawable background, icon, slot;

    public TrailJeiCategory(IGuiHelper gui, RecipeType<TrailJeiRecipe> type, Component title, ItemStack icon) {
        this.type = type;
        this.title = title;
        this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);
        this.slot = gui.getSlotDrawable();
    }

    @Override
    public RecipeType<TrailJeiRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    public IDrawable getBackground() {
        return background;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TrailJeiRecipe recipe, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1).addItemStacks(inputs.get(i)).setBackground(slot, -1, -1);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1, 1).addItemStack(recipe.output()).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(TrailJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        UiDraw.arrow(g, ARROW_X, 2);
        Font font = Minecraft.getInstance().font;
        int y = 23;
        for (FormattedCharSequence line : font.split(recipe.note(), WIDTH)) {
            g.drawString(font, line, 0, y, 0xFF404040, false);
            y += 9;
        }
    }
}

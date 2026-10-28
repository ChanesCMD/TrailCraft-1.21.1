package io.github.ChanesCMD.trailcraft.smelting;

import io.github.ChanesCMD.trailcraft.init.ModComponents;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Обожжённая форма (Mold). Пустую кладут в слот формы в окне кувшина с жидким металлом — металл заливается
 * (компонент trailcraft:cast); ПКМ залитой формой — достать слиток или головку, форма остаётся.
 */
public class MoldItem extends Item {
    public final Mold mold;

    public MoldItem(Mold mold, Properties properties) {
        super(properties);
        this.mold = mold;
    }

    public static Metal cast(ItemStack stack) {
        return stack.get(ModComponents.CAST.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Metal metal = cast(stack);
        if (metal == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            ItemStack result = mold.result(metal);
            stack.remove(ModComponents.CAST.get());
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.DECORATED_POT_BREAK, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Metal metal = cast(stack);
        if (metal != null) {
            tooltip.add(Component.translatable("tooltip.trailcraft.mold.cast", Component.translatable(metal.nameKey()))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.trailcraft.mold.take").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.trailcraft.mold.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}

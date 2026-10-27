package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Удар кремнёвой киркой по породе или руде, которую она не берёт (всё, что для кирки, кроме
 * #trailcraft:mineable/flint_pickaxe), — подсказка над хотбаром, что кирка слишком слаба.
 */
@EventBusSubscriber(modid = TrailCraft.MODID)
public final class FlintPickaxeHint {
    private static final ResourceLocation FLINT_PICKAXE = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "flint_pickaxe");

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !event.getItemStack().is(BuiltInRegistries.ITEM.get(FLINT_PICKAXE))) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(ModTiers.MINEABLE_WITH_FLINT_PICKAXE)) {
            event.getEntity().displayClientMessage(
                    Component.translatable("message.trailcraft.flint_pickaxe.too_weak").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private FlintPickaxeHint() {}
}

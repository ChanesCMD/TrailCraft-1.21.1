package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.water.Water;
import io.github.ChanesCMD.trailcraft.water.WaterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * ПКМ пустой рукой по воде — глоток. Ванильный прицел воду пропускает (видит дно), поэтому луч пускаем сами,
 * с учётом жидкостей; если первой на пути вода — пьём вместо действия с блоком за ней.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientWaterEvents {

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            tryDrink(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide && event.getHand() == InteractionHand.MAIN_HAND && tryDrink(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean tryDrink(Player player) {
        if (player != Minecraft.getInstance().player || !player.getMainHandItem().isEmpty()
                || player.isCreative() || player.isSpectator() || WaterEvents.get(player) >= Water.MAX) {
            return false;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1, true);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && player.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
            player.swing(InteractionHand.MAIN_HAND);
            PacketDistributor.sendToServer(Water.DrinkPayload.INSTANCE);
            return true;
        }
        return false;
    }
}

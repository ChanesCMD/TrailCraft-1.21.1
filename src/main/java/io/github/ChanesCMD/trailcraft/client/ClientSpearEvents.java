package io.github.ChanesCMD.trailcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.init.ModTools;
import io.github.ChanesCMD.trailcraft.item.SpearItem;
import io.github.ChanesCMD.trailcraft.spear.SpearCombat;
import io.github.ChanesCMD.trailcraft.spear.SpearJabPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Копьё на клиенте: ЛКМ — укол вместо обычной атаки, разгон без замедления, анимации от первого лица. */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public class ClientSpearEvents {
    /** 1, пока копьём разгоняются: модель в руке переключается на опущенную (overrides в модели копья). */
    private static final ResourceLocation CHARGING = ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID, "charging");

    /** ЛКМ с копьём в руке: обычную атаку и копание отменяем; при полной зарядке — укол. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!event.isAttack() || player == null || !(player.getMainHandItem().getItem() instanceof SpearItem)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (player.isUsingItem() || player.isSpectator() || player.getAttackStrengthScale(0) < 1) {
            return;
        }
        int lunge = SpearCombat.lungeLevel(player, player.getMainHandItem());
        player.swing(InteractionHand.MAIN_HAND);
        player.resetAttackStrengthTicker();
        PacketDistributor.sendToServer(SpearJabPayload.INSTANCE);
        if (lunge > 0) {
            Vec3 impulse = SpearCombat.lungeImpulse(player, lunge);
            player.push(impulse.x, impulse.y, impulse.z);
        }
    }

    /** Ванилла замедляет игрока с предметом в руке в 5 раз; с копьём разгоняются на бегу, поэтому возвращаем скорость. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        if (player.isUsingItem() && !player.isPassenger() && player.getUseItem().getItem() instanceof SpearItem) {
            event.getInput().forwardImpulse *= 5;
            event.getInput().leftImpulse *= 5;
        }
    }

    private static boolean isSpear(Item item) {
        return item instanceof SpearItem;
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ModTools.ITEMS.stream().map(item -> item.get()).filter(ClientSpearEvents::isSpear).forEach(item ->
                    ItemProperties.register(item, CHARGING, (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0)));
        }

        @SubscribeEvent
        public static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(new SpearHand(), ModTools.ITEMS.stream().map(item -> item.get())
                    .filter(ClientSpearEvents::isSpear).toArray(Item[]::new));
        }
    }

    /** Положение копья от первого лица: укол — тычок вперёд вместо взмаха; разгон — копьё выставлено вперёд. */
    private static class SpearHand implements IClientItemExtensions {
        /** От третьего лица: укол — выпад рукой вперёд (SpearArmPose). */
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return SpearArmPose.get();
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                               float partialTick, float equipProcess, float swingProcess) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(side * 0.56f, -0.52f + equipProcess * -0.6f, -0.72f);
            if (player.isUsingItem() && player.getUseItem() == stack && stack.getItem() instanceof SpearItem spear) {
                int used = player.getTicksUsingItem();
                float time = used + partialTick;
                float raise = Mth.clamp(time / SpearItem.ticks(spear.stats().delay()), 0, 1);
                poseStack.translate(-side * 0.08f * raise, 0.06f * raise, -0.15f * raise);
                switch (spear.stage(used)) {
                    // Устал: копьё дрожит
                    case 2 -> poseStack.translate(Mth.sin(time * 2.1f) * 0.006f, Mth.cos(time * 2.7f) * 0.006f, 0);
                    // Третья стадия и после: копьё опущено
                    case 3, 4 -> poseStack.translate(0, -0.25f, 0.05f);
                    default -> {}
                }
            } else if (swingProcess > 0) {
                float thrust = Mth.sin(swingProcess * Mth.PI);
                poseStack.translate(-side * 0.05f * thrust, 0.04f * thrust, -0.4f * thrust);
            }
            return true;
        }
    }
}

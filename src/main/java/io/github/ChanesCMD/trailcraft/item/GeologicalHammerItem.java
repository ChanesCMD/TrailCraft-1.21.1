package io.github.ChanesCMD.trailcraft.item;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.block.OreBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Геологический молоток, как кирка геолога в TFC: ПКМ по блоку — над хотбаром, какой руды в кубе ±{@link #RADIUS}
 * блоков вокруг больше всего и сколько (следы, немного, средне, много, очень много — {@link #AMOUNTS}), или что руды нет.
 * По самой руде — какая это руда и в какой породе. Тратит прочность, между ударами — {@link #COOLDOWN} тиков.
 */
public class GeologicalHammerItem extends Item {
    public static final int DURABILITY = 128;
    public static final int RADIUS = 12;
    public static final int COOLDOWN = 10;
    /** Сколько блоков руды нужно для «немного», «средне», «много», «очень много»; меньше первого — «следы». */
    private static final int[] AMOUNTS = {5, 10, 20, 40};
    private static final String[] AMOUNT_KEYS = {"traces", "small", "medium", "large", "very_large"};

    public GeologicalHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        level.playSound(null, pos, clicked.getSoundType(level, pos, player).getHitSound(), SoundSource.PLAYERS, 1.0f, 1.2f);
        player.displayClientMessage(report(level, pos, clicked), true);
        return InteractionResult.CONSUME;
    }

    private static Component report(Level level, BlockPos pos, BlockState clicked) {
        if (clicked.getBlock() instanceof OreBlock ore) {
            Block rock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(TrailCraft.MODID,
                    ore.rock(clicked).getSerializedName()));
            return Component.translatable("item.trailcraft.geological_hammer.this", ore.getName(), rock.getName());
        }
        Map<Block, Integer> counts = new HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -RADIUS, -RADIUS), pos.offset(RADIUS, RADIUS, RADIUS))) {
            if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof OreBlock ore) {
                counts.merge(ore, 1, Integer::sum);
            }
        }
        var best = counts.entrySet().stream().max(Map.Entry.comparingByValue());
        if (best.isEmpty()) {
            return Component.translatable("item.trailcraft.geological_hammer.nothing");
        }
        int amount = 0;
        while (amount < AMOUNTS.length && best.get().getValue() >= AMOUNTS[amount]) {
            amount++;
        }
        return Component.translatable("item.trailcraft.geological_hammer.found",
                Component.translatable("item.trailcraft.geological_hammer." + AMOUNT_KEYS[amount]), best.get().getKey().getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.trailcraft.geological_hammer.desc", RADIUS).withStyle(ChatFormatting.GRAY));
    }
}

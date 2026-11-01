package io.github.ChanesCMD.trailcraft.cushion;

import io.github.ChanesCMD.trailcraft.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Подушка в руке: ПКМ по верхней грани блока ставит CushionEntity, как в 26.3: ровно по центру блока по X и Z, без
 * поворота, а по высоте — в точку клика с шагом 1/16 блока (попиксельно, на плите ниже, на ковре чуть выше). Ставится и в
 * блок (например, в нишу), нельзя только на другую подушку. TODO: уточнить, нужна ли установка на боковые грани и потолок (в 26.3 только на плоскую опору).
 */
public class CushionItem extends Item {
    private final DyeColor color;

    public CushionItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Vec3 hit = context.getClickLocation();
        BlockPos clicked = context.getClickedPos();
        double x = clicked.getX() + 0.5, y = snap(hit.y), z = clicked.getZ() + 0.5;
        double half = CushionEntity.WIDTH / 2;
        AABB box = new AABB(x - half, y, z - half, x + half, y + CushionEntity.HEIGHT, z + half);
        if (!CushionEntity.isSupported(level, null, box)
                || !level.getEntitiesOfClass(CushionEntity.class, box.deflate(1.0E-3)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            CushionEntity cushion = ModEntities.CUSHION.get().create(level);
            if (cushion == null) {
                return InteractionResult.FAIL;
            }
            cushion.moveTo(x, y, z, 0, 0);
            cushion.setColor(color);
            level.addFreshEntity(cushion);
            level.playSound(null, x, y, z, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, cushion.position());
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Ближайший пиксель (1/16 блока). */
    private static double snap(double value) {
        return Mth.floor(value * 16 + 0.5) / 16.0;
    }
}

package io.github.ChanesCMD.trailcraft.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

/**
 * Дёрнорез: инструмент типа лопаты. Блоки с дёрном, добытые им, выпадают вместе с дёрном
 * (это задаётся в таблицах лута блоков *_grass через условие match_tool).
 */
public class TurfCutterItem extends DiggerItem {
    public TurfCutterItem(Tier tier, Item.Properties properties) {
        super(tier, BlockTags.MINEABLE_WITH_SHOVEL, properties);
    }
}

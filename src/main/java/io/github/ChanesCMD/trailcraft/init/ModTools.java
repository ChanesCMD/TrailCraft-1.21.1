package io.github.ChanesCMD.trailcraft.init;

import io.github.ChanesCMD.trailcraft.init.ModTiers.Material;
import io.github.ChanesCMD.trailcraft.item.SpearItem;
import io.github.ChanesCMD.trailcraft.item.TieredToolItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Инструменты и предметы из ТЗ, часть 2 (52 предмета). Имя в реестре = имя текстуры.
 * Кирка, топор, лопата — кремень и металлы; нож и копьё — все материалы; серп, коса, пила, кусачки — металлы.
 * Кремнёвая «лопата» (flint_shovel) в игре называется «Кремнёвая мотыга».
 * TODO: уточнить, заменять ли ванильные железные инструменты (сейчас свои рядом с ванильными).
 */
public final class ModTools {
    /** Все предметы в порядке творческой вкладки. */
    public static final List<DeferredItem<? extends Item>> ITEMS = new ArrayList<>();

    static {
        for (Material m : Material.values()) {
            if (m == Material.FLINT) {
                // Кремнёвая кирка добывает только свой список блоков (гравий и начальные руды), см. MINEABLE_WITH_FLINT_PICKAXE
                reg(m.id + "_pickaxe", p -> new DiggerItem(m.tier, ModTiers.MINEABLE_WITH_FLINT_PICKAXE,
                        p.attributes(DiggerItem.createAttributes(m.tier, ModTiers.PICKAXE_DAMAGE, ModTiers.PICKAXE_SPEED))));
            } else if (m.isMetal()) {
                reg(m.id + "_pickaxe", p -> new PickaxeItem(m.tier,
                        p.attributes(DiggerItem.createAttributes(m.tier, ModTiers.PICKAXE_DAMAGE, ModTiers.PICKAXE_SPEED))));
            }
        }
        for (Material m : Material.values()) {
            if (m == Material.FLINT || m.isMetal()) {
                reg(m.id + "_axe", p -> new AxeItem(m.tier, p.attributes(DiggerItem.createAttributes(m.tier, m.axeDamage, m.axeSpeed))));
            }
        }
        for (Material m : Material.values()) {
            if (m == Material.FLINT || m.isMetal()) {
                reg(m.id + "_shovel", p -> new ShovelItem(m.tier,
                        p.attributes(DiggerItem.createAttributes(m.tier, ModTiers.SHOVEL_DAMAGE, ModTiers.SHOVEL_SPEED))));
            }
        }
        for (Material m : Material.values()) {
            reg(m.id + "_knife", p -> new SwordItem(m.tier,
                    p.attributes(SwordItem.createAttributes(m.tier, ModTiers.KNIFE_DAMAGE, ModTiers.KNIFE_SPEED))));
        }
        for (Material m : Material.values()) {
            reg(m.id + "_spear", p -> new SpearItem(m.tier, m.spear, p.attributes(SpearItem.createAttributes(m.spear))));
        }
        for (Material m : Material.values()) {
            if (m.isMetal()) {
                tool(m, "_sickle", ModTiers.SICKLE_DAMAGE, ModTiers.SICKLE_SPEED);
                tool(m, "_scythe", ModTiers.SCYTHE_DAMAGE, ModTiers.SCYTHE_SPEED);
                tool(m, "_saw", ModTiers.SAW_DAMAGE, ModTiers.SAW_SPEED);
                tool(m, "_cutters", ModTiers.CUTTERS_DAMAGE, ModTiers.CUTTERS_SPEED);
            }
        }

        // TODO: уточнить, что делают лейки, линейка и метла
        single("wooden_watering_can");
        single("bronze_watering_can");
        single("zinc_watering_can");
        single("wooden_ruler");
        single("broom");
    }

    /** Вызывается из конструктора мода, чтобы статический блок выше выполнился до событий регистрации. */
    public static void init() {}

    private static void reg(String name, Function<Item.Properties, ? extends Item> factory) {
        ITEMS.add(ModItems.ITEMS.registerItem(name, factory));
    }

    private static void tool(Material m, String suffix, float damage, float speed) {
        reg(m.id + suffix, p -> new TieredToolItem(m.tier, p.attributes(DiggerItem.createAttributes(m.tier, damage, speed))));
    }

    private static void single(String name) {
        ITEMS.add(ModItems.ITEMS.registerSimpleItem(name, new Item.Properties().stacksTo(1)));
    }

    private ModTools() {}
}

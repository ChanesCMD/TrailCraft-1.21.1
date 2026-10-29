package io.github.ChanesCMD.trailcraft.ore;

import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.List;

import static io.github.ChanesCMD.trailcraft.ore.Rock.*;

/**
 * Все руды в одном месте: из этого списка регистрируются блоки, их предметы и то, что с них падает (ModOres).
 * JSON (модели, блокстейты, лут, тэги, генерация, переводы) пишет scripts/gen_ores.py по такой же таблице —
 * меняя руду здесь, поменяйте её и там.
 * <p>
 * Блок один на руду, порода — свойство {@code rock}: малахит в известняке — {@code malachite_ore[rock=limestone]}.
 */
public enum Ore {
    // id, элемент (для тэгов c:ores/...), какая кирка нужна, что падает, сколько (без удачи), породы (первая — по умолчанию)
    BITUMINOUS_COAL("bituminous_coal", "coal", Tool.ANY, "bituminous_coal", 2, 4, SANDSTONE, LIMESTONE),
    MALACHITE("malachite", "copper", Tool.STONE, "raw_malachite", 2, 4, LIMESTONE, DOLOMITE),
    NATIVE_COPPER("native_copper", "copper", Tool.STONE, "raw_native_copper", 1, 1, BASALT, GABBRO),
    TETRAHEDRITE("tetrahedrite", "copper", Tool.STONE, "raw_tetrahedrite", 2, 4, ANDESITE, GRANITE),
    HEMATITE("hematite", "iron", Tool.STONE, "raw_hematite", 1, 1, SANDSTONE, SHALE),
    MAGNETITE("magnetite", "iron", Tool.STONE, "raw_magnetite", 1, 1, DIORITE, MARBLE),
    LIMONITE("limonite", "iron", Tool.ANY, "raw_limonite", 2, 4, LIMESTONE, CHALK),
    SPHALERITE("sphalerite", "zinc", Tool.STONE, "raw_sphalerite", 2, 4, DOLOMITE, LIMESTONE),
    CASSITERITE("cassiterite", "tin", Tool.STONE, "raw_cassiterite", 2, 4, GRAY_GRANITE, GRANITE),
    NATIVE_BISMUTH("native_bismuth", "bismuth", Tool.STONE, "raw_native_bismuth", 1, 1, GRANITE, MARBLE),
    GALENA("galena", "lead", Tool.STONE, "raw_galena", 2, 4, LIMESTONE, MARBLE),
    NATIVE_GOLD("native_gold", "gold", Tool.IRON, "raw_native_gold", 1, 1, GRAY_GRANITE, ANDESITE),
    PENTLANDITE("pentlandite", "nickel", Tool.IRON, "raw_pentlandite", 1, 1, GABBRO, BASALT),
    LEPIDOLITE("lepidolite", "lithium", Tool.STONE, "raw_lepidolite", 1, 1, GRANITE, GRAY_GRANITE),
    BAUXITE("bauxite", "aluminum", Tool.ANY, "raw_bauxite", 2, 4, LIMESTONE, BASALT),
    NEPHELINE("nepheline", "aluminum", Tool.STONE, "raw_nepheline", 2, 4, DIORITE, GRAY_GRANITE),
    APATITE("apatite", "apatite", Tool.STONE, "apatite", 2, 4, GABBRO, MARBLE),
    CINNABAR("cinnabar", "redstone", Tool.STONE, "raw_cinnabar", 1, 2, SHALE, BASALT),
    /** С кварцевой жилы падает ванильный кварц (dropId с пространством имён — свой предмет не регистрируется). */
    QUARTZ("quartz", "quartz", Tool.STONE, "minecraft:quartz", 1, 2, GRANITE, GRAY_GRANITE);

    /** Какая кирка добывает руду: любая (кроме кремнёвой), от каменного уровня (медь), от железного уровня. */
    public enum Tool { ANY, STONE, IRON }

    public final String id;
    public final String element;
    public final Tool tool;
    public final String dropId;
    public final int minDrop, maxDrop;
    public final List<Rock> rocks;
    public final EnumProperty<Rock> rockProperty;

    Ore(String id, String element, Tool tool, String dropId, int minDrop, int maxDrop, Rock... rocks) {
        this.id = id;
        this.element = element;
        this.tool = tool;
        this.dropId = dropId;
        this.minDrop = minDrop;
        this.maxDrop = maxDrop;
        this.rocks = List.of(rocks);
        this.rockProperty = EnumProperty.create("rock", Rock.class, rocks);
    }

    public String blockId() {
        return id + "_ore";
    }
}

package io.github.ChanesCMD.trailcraft.smelting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ChanesCMD.trailcraft.smelting.Smelting.Metal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Жидкий металл в кувшине (компонент trailcraft:molten, как в TerraFirmaCraft): какой металл и сколько, в mB
 * (Smelting.UNIT на один слиток). Его разливают по формам (JugMenu).
 */
public record Molten(Metal metal, int amount) {
    public static final Codec<Molten> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Metal.CODEC.fieldOf("metal").forGetter(Molten::metal),
            Codec.INT.fieldOf("amount").forGetter(Molten::amount)).apply(instance, Molten::new));
    public static final StreamCodec<ByteBuf, Molten> STREAM_CODEC = StreamCodec.composite(
            Metal.STREAM_CODEC, Molten::metal, ByteBufCodecs.VAR_INT, Molten::amount, Molten::new);

    /** «Жидкая медь, 400 mB». */
    public MutableComponent describe() {
        return Component.translatable("tooltip.trailcraft.molten", Component.translatable(metal.moltenKey()), amount);
    }
}

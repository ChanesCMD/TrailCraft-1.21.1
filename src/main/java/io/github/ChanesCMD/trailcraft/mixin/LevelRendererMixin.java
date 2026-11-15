package io.github.ChanesCMD.trailcraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ChanesCMD.trailcraft.client.TreeSnow;
import io.github.ChanesCMD.trailcraft.client.weather.WeatherParticles;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Погода частицами (client/weather/WeatherParticles): ванильные полосы дождя и снега не рисуются, а в tickRain
 * не спавнятся ванильные брызги и дымок (их заменяют свои). Звук дождя из tickRain остаётся.
 * Снег появился на дереве или пропал (blockChanged) — client/TreeSnow сбрасывает кеш и перестраивает крону.
 * Облака рисуются дальше ванильного: площадка buildClouds расширена с 8 клеток (768 блоков) до 65 (6300 блоков)
 * для FANCY, а для FAST (плоский слой) строится своя площадка на 6144 блока — ванильная была всего 768.
 * Туман на время отрисовки облаков отодвинут к краю площадки, иначе шейдер режет их по render distance.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /** Полуширина площадки облаков в клетках по 8 единиц (96 блоков). Ваниль — 3 (768 блоков), тут 32 (6300). */
    private static final int TRAILCRAFT_CLOUD_CELLS = 32;
    /** Плоский слой (FAST) — квады по 32 единицы (384 блока), полуширина в единицах: 256 = 3072 блока. */
    private static final int TRAILCRAFT_FAST_CELLS = 256;
    /** Туман облаков: до 2400 блоков их не трогает вовсе, к 3000 (край площадки) они растворяются в небе. */
    private static final float TRAILCRAFT_CLOUD_FOG_START = 2400.0F;
    private static final float TRAILCRAFT_CLOUD_FOG_END = 3000.0F;
    private float trailcraft$savedFogStart;
    private float trailcraft$savedFogEnd;

    @Shadow
    private CloudStatus prevCloudsType;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void trailcraft$particleWeather(CallbackInfo ci) {
        if (WeatherParticles.enabled()) {
            ci.cancel();
        }
    }

    // Облака: в buildClouds клетки идут циклом -3..4, здесь границы заменяются на -N..N+1.
    // require = 0: если ванильные константы вдруг изменятся, облака просто останутся ванильными.
    @ModifyConstant(method = "buildClouds", require = 0, constant = @Constant(intValue = -3))
    private int trailcraft$cloudFieldStart(int original) {
        return -TRAILCRAFT_CLOUD_CELLS;
    }

    @ModifyConstant(method = "buildClouds", require = 0, constant = @Constant(intValue = 4))
    private int trailcraft$cloudFieldEnd(int original) {
        return TRAILCRAFT_CLOUD_CELLS + 1;
    }

    // Ванильные «быстрые» облака — это одна плоскость 768x768 блоков (квады по 32 единицы от -32 до 32),
    // она и обрывалась в 384 блоках впереди. В этом режиме строим свою плоскость того же вида, но шире.
    @Inject(method = "buildClouds", at = @At("HEAD"), cancellable = true)
    private void trailcraft$wideFastClouds(Tesselator tesselator, double x, double y, double z, Vec3 cloudColor,
                                           CallbackInfoReturnable<MeshData> cir) {
        if (this.prevCloudsType != CloudStatus.FAST) {
            return;
        }
        float uvX = (float) Mth.floor(x) * 0.00390625F;
        float uvZ = (float) Mth.floor(z) * 0.00390625F;
        float cloudY = (float) Math.floor(y / 4.0) * 4.0F;
        float red = (float) cloudColor.x;
        float green = (float) cloudColor.y;
        float blue = (float) cloudColor.z;
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        for (int px = -TRAILCRAFT_FAST_CELLS; px < TRAILCRAFT_FAST_CELLS; px += 32) {
            for (int pz = -TRAILCRAFT_FAST_CELLS; pz < TRAILCRAFT_FAST_CELLS; pz += 32) {
                quad(buffer, cloudY, px, pz, uvX, uvZ, red, green, blue);
            }
        }
        cir.setReturnValue(buffer.buildOrThrow());
    }

    private static void quad(BufferBuilder buffer, float y, float x, float z, float uvX, float uvZ,
                             float red, float green, float blue) {
        float u0 = x * 0.00390625F + uvX;
        float u1 = (x + 32.0F) * 0.00390625F + uvX;
        float v0 = z * 0.00390625F + uvZ;
        float v1 = (z + 32.0F) * 0.00390625F + uvZ;
        buffer.addVertex(x, y, z + 32.0F).setUv(u0, v1).setColor(red, green, blue, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
        buffer.addVertex(x + 32.0F, y, z + 32.0F).setUv(u1, v1).setColor(red, green, blue, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
        buffer.addVertex(x + 32.0F, y, z).setUv(u1, v0).setColor(red, green, blue, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
        buffer.addVertex(x, y, z).setUv(u0, v0).setColor(red, green, blue, 0.8F).setNormal(0.0F, -1.0F, 0.0F);
    }

    // Туман для облаков: шейдер rendertype_clouds.fsh красит их через FogStart/FogEnd из render distance,
    // из-за этого облака таяли уже на ~200 блоках. На время отрисовки облаков туман отодвигается к краю
    // площадки, после — возвращается прежний (иначе без тумана остались бы дождь и частицы).
    @Inject(method = "renderClouds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V"))
    private void trailcraft$cloudFogFar(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                        float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        this.trailcraft$savedFogStart = RenderSystem.getShaderFogStart();
        this.trailcraft$savedFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(TRAILCRAFT_CLOUD_FOG_START);
        RenderSystem.setShaderFogEnd(TRAILCRAFT_CLOUD_FOG_END);
    }

    @Inject(method = "renderClouds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V", shift = At.Shift.AFTER))
    private void trailcraft$cloudFogBack(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                         float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        RenderSystem.setShaderFogStart(this.trailcraft$savedFogStart);
        RenderSystem.setShaderFogEnd(this.trailcraft$savedFogEnd);
    }

    @Redirect(method = "tickRain", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void trailcraft$noVanillaRainParticles(ClientLevel level, ParticleOptions particle,
                                                   double x, double y, double z, double xd, double yd, double zd) {
        if (!WeatherParticles.enabled()) {
            level.addParticle(particle, x, y, z, xd, yd, zd);
        }
    }

    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void trailcraft$treeSnow(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        TreeSnow.onBlockChanged(pos, oldState, newState);
    }
}

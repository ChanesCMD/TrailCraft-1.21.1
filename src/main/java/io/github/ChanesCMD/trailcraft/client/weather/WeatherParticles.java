package io.github.ChanesCMD.trailcraft.client.weather;

import io.github.ChanesCMD.trailcraft.TrailCraft;
import io.github.ChanesCMD.trailcraft.client.ClientConfig;
import io.github.ChanesCMD.trailcraft.init.ModParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Погода частицами (по мотивам мода Particle Rain): вместо ванильной текстуры дождя и снега
 * (её убирает mixin/LevelRendererMixin) вокруг камеры спавнятся капли, снежинки и пыль.
 * <ul>
 *   <li>дождь — капли косые по ветру ({@link WeatherWind}), бьются брызгами о землю и кругами о воду,
 *       крона деревьев задерживает большую часть капель;</li>
 *   <li>снег — там, где биом даёт снег (холодно или высоко в горах), снежинки кружатся и ложатся;</li>
 *   <li>пыльная буря — в жарких биомах без осадков (пустыни) дождь мира становится бурей: пыль цвета земли
 *       несётся по ветру, туман сгущается и желтеет;</li>
 *   <li>в дождь и снег туман ближе, в грозу — ещё ближе, ветер сильнее.</li>
 * </ul>
 * Плотность — ClientConfig.WEATHER_DENSITY и ванильная настройка частиц.
 */
@EventBusSubscriber(modid = TrailCraft.MODID, value = Dist.CLIENT)
public final class WeatherParticles {
    /** Радиус вокруг камеры, в котором идёт дождь, и высота слоя над/под камерой. */
    static final float RADIUS = 22, ABOVE = 20, BELOW = 6;
    /** Попыток спавна за тик при полном дожде (снега и пыли — доля от неё, они живут дольше). */
    static final int RAIN_PER_TICK = 70;
    static final float SNOW_SHARE = 0.35f, DUST_SHARE = 0.6f;
    /** Скорость падения капли и снежинки (блоков/тик) и снос ветром на единицу силы ветра. */
    static final float RAIN_FALL = 0.95f, RAIN_DRIFT = 0.45f, SNOW_FALL = 0.07f, SNOW_DRIFT = 0.12f;
    /** Капля с листвы: шанс 1 из стольких за animateTick (как у ванильной) и скорость падения. */
    static final int LEAF_DRIP_CHANCE = 15;
    static final float LEAF_DRIP_FALL = 0.3f;
    /** Биом без осадков с температурой выше этой — пустыня: вместо дождя пыльная буря. */
    static final float DUST_TEMPERATURE = 1.0f;
    /** Насколько сгущается туман: в дождь/снег, в грозу дополнительно, в пыльную бурю. */
    static final float FOG_WET = 0.3f, FOG_THUNDER = 0.2f, FOG_DUST = 0.55f;

    /** Сглаженные (раз в тик) сила тумана и цвет пыли — чтобы на границе биомов туман не прыгал. */
    private static float wetFog, dustFog;
    private static float dustR = 0.84f, dustG = 0.72f, dustB = 0.5f;

    public static boolean enabled() {
        return ClientConfig.weatherParticles();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || !enabled()) {
            wetFog = dustFog = 0;
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float rain = level.getRainLevel(1);
        updateFog(level, cam, rain);
        if (rain <= 0 || !level.dimensionType().hasSkyLight()) {
            return;
        }
        float density = ClientConfig.weatherDensity() * switch (mc.options.particles().get()) {
            case ALL -> 1f;
            case DECREASED -> 0.5f;
            case MINIMAL -> 0.2f;
        };
        float thunder = level.getThunderLevel(1);
        Vec3 wind = WeatherWind.vector(level, 0);
        RandomSource random = level.random;
        int attempts = Mth.ceil(RAIN_PER_TICK * rain * (1 + 0.5f * thunder) * density);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            double r = RADIUS * Math.sqrt(random.nextDouble());
            double a = random.nextDouble() * Mth.TWO_PI;
            double x = cam.x + Math.cos(a) * r, z = cam.z + Math.sin(a) * r;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double lo = Math.max(top, cam.y - BELOW), hi = cam.y + ABOVE;
            if (lo >= hi || !level.hasChunkAt(pos.set(x, cam.y, z))) {
                continue;
            }
            double y = lo + random.nextDouble() * (hi - lo);
            // биом — у земли, как у ванильного дождя (иначе /fillbiome тонким слоем не даёт снега над головой)
            Biome biome = level.getBiome(pos.set(x, top, z)).value();
            switch (biome.getPrecipitationAt(pos)) {
                case RAIN -> {
                    double vx = wind.x * RAIN_DRIFT, vz = wind.z * RAIN_DRIFT, vy = -RAIN_FALL - random.nextFloat() * 0.25;
                    // спавним с наветренной стороны, чтобы капля упала туда, где проверена высота земли
                    double fall = (y - top) / -vy;
                    spawn(mc, ModParticles.RAIN_DROP.get(), x - vx * fall, y, z - vz * fall, vx, vy, vz);
                }
                case SNOW -> {
                    if (random.nextFloat() < SNOW_SHARE) {
                        double vy = -SNOW_FALL - random.nextFloat() * 0.03;
                        spawn(mc, ModParticles.SNOW_FLAKE.get(), x, y, z,
                                wind.x * SNOW_DRIFT, vy, wind.z * SNOW_DRIFT);
                    }
                }
                case NONE -> {
                    if (biome.getBaseTemperature() > DUST_TEMPERATURE && random.nextFloat() < DUST_SHARE
                            && top > cam.y - 12 && top < cam.y + 12) {
                        int color = groundColor(level, pos.set(x, top - 1, z));
                        spawn(mc, ModParticles.DUST_MOTE.get(), x, top + random.nextDouble() * 4, z,
                                (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
                    }
                }
            }
        }
    }

    /**
     * Капля с листвы в дождь — как ванильная (так же часто и только над пустотой), но своя: медленная полоса,
     * которая разбивается брызгами о землю.
     */
    public static void leafDrip(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(LEAF_DRIP_CHANCE) != 0 || !level.isRainingAt(pos.above())) {
            return;
        }
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        if (state.canOcclude() && state.isFaceSturdy(level, below, Direction.UP)) {
            return;
        }
        Minecraft.getInstance().particleEngine.createParticle(ModParticles.RAIN_DROP.get(),
                pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                0, -LEAF_DRIP_FALL, 0);
    }

    private static void spawn(Minecraft mc, net.minecraft.core.particles.ParticleOptions type,
                              double x, double y, double z, double xd, double yd, double zd) {
        mc.particleEngine.createParticle(type, x, y, z, xd, yd, zd);
    }

    /** Цвет земли (как на карте): песок, красный песок, жёлтозём… Для блоков без цвета — песочный. */
    private static int groundColor(ClientLevel level, BlockPos pos) {
        int color = level.getBlockState(pos).getMapColor(level, pos).col;
        return color == 0 ? 0xD6B678 : color;
    }

    private static void updateFog(ClientLevel level, Vec3 cam, float rain) {
        BlockPos pos = BlockPos.containing(cam);
        Biome biome = level.getBiome(pos).value();
        Biome.Precipitation precipitation = biome.getPrecipitationAt(pos);
        boolean dusty = precipitation == Biome.Precipitation.NONE && biome.getBaseTemperature() > DUST_TEMPERATURE;
        float wetTarget = precipitation == Biome.Precipitation.NONE ? 0
                : rain * (precipitation == Biome.Precipitation.SNOW ? 1f : 0.6f) * (1 + level.getThunderLevel(1));
        float dustTarget = dusty ? rain : 0;
        wetFog += (wetTarget - wetFog) * 0.02f;
        dustFog += (dustTarget - dustFog) * 0.02f;
        if (dusty) {
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            int color = groundColor(level, new BlockPos(pos.getX(), top - 1, pos.getZ()));
            dustR += ((color >> 16 & 255) / 255f - dustR) * 0.05f;
            dustG += ((color >> 8 & 255) / 255f - dustG) * 0.05f;
            dustB += ((color & 255) / 255f - dustB) * 0.05f;
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!enabled() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE
                || wetFog + dustFog < 0.01f) {
            return;
        }
        float k = (1 - FOG_WET * Math.min(wetFog, 1) - FOG_THUNDER * Math.max(0, wetFog - 1)) * (1 - FOG_DUST * dustFog);
        event.setFarPlaneDistance(event.getFarPlaneDistance() * k);
        event.setNearPlaneDistance(event.getNearPlaneDistance() * k * (1 - 0.5f * dustFog));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!enabled() || dustFog < 0.01f || isUnderFluid(event.getCamera())) {
            return;
        }
        float t = dustFog * 0.6f;
        event.setRed(Mth.lerp(t, event.getRed(), dustR * 0.9f));
        event.setGreen(Mth.lerp(t, event.getGreen(), dustG * 0.9f));
        event.setBlue(Mth.lerp(t, event.getBlue(), dustB * 0.9f));
    }

    private static boolean isUnderFluid(Camera camera) {
        return camera.getFluidInCamera() != FogType.NONE;
    }

    @EventBusSubscriber(modid = TrailCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void onRegisterProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.RAIN_DROP.get(), RainDropParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SNOW_FLAKE.get(), SnowFlakeParticle.Provider::new);
            event.registerSpriteSet(ModParticles.RAIN_SPLASH.get(), RainSplashParticle.Provider::new);
            event.registerSpriteSet(ModParticles.RAIN_RIPPLE.get(), RainRippleParticle.Provider::new);
            event.registerSpriteSet(ModParticles.DUST_MOTE.get(), DustMoteParticle.Provider::new);
        }
    }

    private WeatherParticles() {}
}

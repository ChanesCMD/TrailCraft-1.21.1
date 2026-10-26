package io.github.ChanesCMD.trailcraft.client.gui;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Доступ к внутренностям JEI, которых нет в его публичном API: режим читера и «список предметов только при
 * поиске» (то, что делала шестерёнка JEI). Всё через рефлексию и в try/catch: если JEI другой версии, функция просто
 * недоступна (available() == false), игра не падает.
 */
public final class JeiBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean failed;

    /** Включён ли режим читера JEI. */
    public static boolean cheatMode() {
        Object state = toggleState();
        return state != null && Boolean.TRUE.equals(call(state, "mezz.jei.common.config.IClientToggleState", "isCheatItemsEnabled"));
    }

    /** Переключает режим читера так же, как шестерёнка JEI. @return получилось ли */
    public static boolean toggleCheatMode() {
        Object state = toggleState();
        if (state == null) {
            return false;
        }
        try {
            Class<?> stateType = Class.forName("mezz.jei.common.config.IClientToggleState");
            Class.forName("mezz.jei.gui.util.CheatModeUtil").getMethod("toggleCheatMode", stateType).invoke(null, state);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    /** Список предметов JEI показывается только при поиске; null, если JEI не отвечает. */
    @Nullable
    public static Boolean hideWhenNotSearching() {
        Object value = hideValue();
        return value == null ? null : (Boolean) call(value, "net.mezzdev.config.api.value.IConfigValue", "get");
    }

    /** Переключает «список только при поиске». @return получилось ли */
    public static boolean toggleHideWhenNotSearching() {
        Object value = hideValue();
        Boolean current = hideWhenNotSearching();
        if (value == null || current == null) {
            return false;
        }
        try {
            Class.forName("net.mezzdev.config.api.value.IConfigValue").getMethod("set", Object.class).invoke(value, !current);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    @Nullable
    private static Object toggleState() {
        return failed ? null : call(null, "mezz.jei.common.Internal", "getClientToggleState");
    }

    @Nullable
    private static Object hideValue() {
        if (failed) {
            return null;
        }
        Object configs = call(null, "mezz.jei.common.Internal", "getClientConfigs");
        Object config = configs == null ? null : call(configs, "mezz.jei.common.config.IClientConfigs", "getClientConfig");
        return config == null ? null : call(config, "mezz.jei.common.config.IClientConfig", "hideIngredientsWhenNotSearching");
    }

    /** Вызывает метод без аргументов; метод ищется у типа (интерфейса или класса), а не у реализации. */
    @Nullable
    private static Object call(@Nullable Object target, String type, String method) {
        try {
            Method m = Class.forName(type).getMethod(method);
            return m.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return null;
        }
    }

    private static void fail(Exception e) {
        if (!failed) {
            failed = true;
            LOGGER.warn("JEI internals are not available, the extra JEI button functions are off", e);
        }
    }

    private JeiBridge() {}
}

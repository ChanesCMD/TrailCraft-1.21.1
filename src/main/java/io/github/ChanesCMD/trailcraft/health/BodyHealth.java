package io.github.ChanesCMD.trailcraft.health;

import net.minecraft.world.entity.player.Player;

/**
 * Здоровье по частям тела для модульного HP в HUD. Самой системы (в духе Barotrauma) пока нет:
 * TODO: заменить заглушку — сейчас все части берут состояние из общего ванильного здоровья.
 */
public final class BodyHealth {
    public enum Part { HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

    /** Состояние части тела; цвета — в HUD. */
    public enum State { INTACT, DAMAGED, CRITICAL, DESTROYED, PROSTHESIS }

    /** Доля здоровья, ниже которой часть считается повреждённой и критической. */
    public static final float DAMAGED_BELOW = 0.66f, CRITICAL_BELOW = 0.33f;

    public static State state(Player player, Part part) {
        float fraction = player.getHealth() / player.getMaxHealth();
        if (fraction <= 0) {
            return State.DESTROYED;
        }
        if (fraction < CRITICAL_BELOW) {
            return State.CRITICAL;
        }
        return fraction < DAMAGED_BELOW ? State.DAMAGED : State.INTACT;
    }

    /** Полное ли здоровье: фигурку в HUD показываем только когда нет. */
    public static boolean isFull(Player player) {
        return player.getHealth() >= player.getMaxHealth();
    }

    private BodyHealth() {}
}

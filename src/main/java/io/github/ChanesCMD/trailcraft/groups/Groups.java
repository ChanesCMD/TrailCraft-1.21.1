package io.github.ChanesCMD.trailcraft.groups;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Party, города и страны — внешние системы мода (ТЗ 5.1). Чат и голосовой чат узнают через этот провайдер,
 * в какой группе игрок. TODO: подключить настоящие системы; пока игрок ни в чём не состоит.
 */
public final class Groups {
    public enum Kind { PARTY, CITY, COUNTRY }

    /** В какой группе данного вида игрок: id группы или null. */
    public interface Provider {
        @Nullable
        String groupOf(ServerPlayer player, Kind kind);
    }

    private static Provider provider = (player, kind) -> null;

    public static void setProvider(Provider newProvider) {
        provider = newProvider;
    }

    @Nullable
    public static String of(ServerPlayer player, Kind kind) {
        return provider.groupOf(player, kind);
    }

    /** Игроки онлайн в той же группе, что и player (включая его), или пусто, если он не в группе. */
    public static List<ServerPlayer> members(ServerPlayer player, Kind kind) {
        String id = of(player, kind);
        if (id == null) {
            return List.of();
        }
        MinecraftServer server = player.getServer();
        return server == null ? List.of() : server.getPlayerList().getPlayers().stream()
                .filter(other -> Objects.equals(id, of(other, kind))).toList();
    }

    private Groups() {}
}

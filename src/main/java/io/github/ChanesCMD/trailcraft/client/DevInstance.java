package io.github.ChanesCMD.trailcraft.client;

import io.github.ChanesCMD.trailcraft.TrailCraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Игра, запущенная из среды разработки (scripts/play.bat), записывает свой номер процесса и время его запуска в файл
 * (run/trailcraft-game.pid; путь задаёт build.gradle через системное свойство trailcraft.pidFile). Новый play.bat
 * по нему находит и закрывает уже запущенную копию (scripts/stop-game.ps1), а при выходе из игры файл удаляется.
 */
public final class DevInstance {
    public static void register(Path file) {
        try {
            ProcessHandle self = ProcessHandle.current();
            long started = self.info().startInstant().map(Instant::toEpochMilli).orElse(0L);
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, self.pid() + "\n" + started + "\n");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // файл просто останется, play.bat проверит процесс по номеру и времени запуска
                }
            }));
        } catch (IOException | RuntimeException e) {
            TrailCraft.LOGGER.warn("Can't write the game PID file {}", file, e);
        }
    }

    private DevInstance() {}
}

# TrailCraft

Мод для Minecraft 1.21.1 на NeoForge (Java 21, modid `trailcraft`). ТЗ: `CLAUDE_TASK.md`, `docs/TASK_ITEMS.md`, `docs/TASK_UI.md`.

## Работа с нескольких устройств

Над проектом работают с двух ПК и с телефона (см. `docs/WORKFLOW.md`), общая ветка — `main`.
- В начале работы — `git pull`, чтобы не разойтись с другим устройством.
- Законченную и собранную правку — закоммитить и запушить, иначе другое устройство её не увидит.
- Мелкие правки без сборки (например, из облачной сессии) проверяются сборкой GitHub Actions после push.

## Сборка и запуск

- Gradle 9.2.1 не запускается на Java 26 (стоит на одном из ПК), нужна JDK 21: `scripts\find-java.bat` выставляет `JAVA_HOME`.
- Сборка: `scripts\build.bat` или `.\gradlew.bat build` с JAVA_HOME на JDK 21; игра: `scripts\play.bat` (`runClient`).
- В bat-файлах только ASCII: путь к проекту содержит кириллицу, а cmd ломает строки с UTF-8.

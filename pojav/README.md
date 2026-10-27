# Моды для PojavLauncher

Папку `mods/` целиком скопировать в папку игры профиля PojavLauncher (или его форка — Amethyst, Zalith и т. п.)
с Minecraft 1.21.1 и NeoForge 21.1.x (в среде разработки — 21.1.259). Java — 21.

Внутри: TrailCraft и его обязательные зависимости — Create, Ponder, Flywheel, Registrate. e4steam, JEI,
MezzConfig, Simple Voice Chat и прочее для разработки сюда не входят (голосовой чат можно доставить отдельно,
он необязательный).

Папку собирает GitHub Actions (`.github/workflows/pojav.yml`, задача Gradle `pojavMods`) после каждого push в `main`,
где менялся код, и сам коммитит её. Вручную: `.\gradlew.bat pojavMods` или запустить workflow «Pojav mods» на
вкладке Actions.

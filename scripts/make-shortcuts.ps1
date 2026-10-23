# Создаёт на рабочем столе ярлыки для этой копии TrailCraft. Запускать через make-shortcuts.bat:
# один раз на каждом ПК и ещё раз, если папку проекта перенесли.
$root = Split-Path -Parent $PSScriptRoot
$desktop = [Environment]::GetFolderPath('Desktop')
$shell = New-Object -ComObject WScript.Shell
$shortcuts = @(
    @{ Name = 'Запустить TrailCraft'; Script = 'play.bat'; Description = 'Обновить TrailCraft с GitHub и запустить игру' },
    @{ Name = 'Собрать TrailCraft'; Script = 'build.bat'; Description = 'Собрать jar мода TrailCraft' }
)
foreach ($s in $shortcuts) {
    $link = $shell.CreateShortcut((Join-Path $desktop "$($s.Name).lnk"))
    $link.TargetPath = Join-Path $PSScriptRoot $s.Script
    $link.WorkingDirectory = $root
    $link.Description = $s.Description
    $link.Save()
    Write-Host "Ярлык на рабочем столе: $($s.Name)"
}

# Closes the TrailCraft game that was started earlier by play.bat and is still running, so that two copies
# never share the run folder (the second one would crash). Called from play.bat.
# The game writes its process id and start time to the PID file (see DevInstance.java).
param([string]$PidFile)

if (-not (Test-Path -LiteralPath $PidFile)) { exit 0 }

$lines = @(Get-Content -LiteralPath $PidFile)
$gamePid = 0
$started = [long]0
if ($lines.Count -lt 2 -or -not [int]::TryParse($lines[0], [ref]$gamePid) -or -not [long]::TryParse($lines[1], [ref]$started)) {
    Remove-Item -LiteralPath $PidFile -ErrorAction SilentlyContinue
    exit 0
}

# The number may belong to another program by now: check the name and the start time too
$proc = Get-Process -Id $gamePid -ErrorAction SilentlyContinue
if ($null -eq $proc -or $proc.ProcessName -notmatch '^javaw?$') {
    Remove-Item -LiteralPath $PidFile -ErrorAction SilentlyContinue
    exit 0
}
if ($started -gt 0) {
    $actual = ([DateTimeOffset]$proc.StartTime).ToUnixTimeMilliseconds()
    if ([Math]::Abs($actual - $started) -gt 5000) {
        Remove-Item -LiteralPath $PidFile -ErrorAction SilentlyContinue
        exit 0
    }
}

# First close the window like the player would (the world is saved), force only if the game does not answer
Write-Host "The game is already running (PID $gamePid), closing it..."
[void]$proc.CloseMainWindow()
if (-not $proc.WaitForExit(30000)) {
    Write-Host "The game did not close in 30 seconds, stopping it."
    Stop-Process -Id $gamePid -Force -ErrorAction SilentlyContinue
    [void]$proc.WaitForExit(10000)
}
Remove-Item -LiteralPath $PidFile -ErrorAction SilentlyContinue

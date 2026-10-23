@echo off
rem Creates the TrailCraft desktop shortcuts for this PC (run once per PC, see make-shortcuts.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0make-shortcuts.ps1"
pause

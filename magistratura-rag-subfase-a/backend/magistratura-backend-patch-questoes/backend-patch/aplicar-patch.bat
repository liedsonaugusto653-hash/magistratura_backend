@echo off
chcp 65001 >nul
echo.
echo === Patch questoes/simulados — Magistratura ===
echo.

REM Se arrastares a pasta backend para cima deste .bat, %1 e o caminho
if not "%~1"=="" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aplicar-patch.ps1" -BackendRoot "%~1"
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aplicar-patch.ps1"
)

echo.
pause

@echo off
setlocal
cd /d "%~dp0"

set FX_LIB=%~dp0lib\javafx\lib
set OUT_DIR=%~dp0out\production\sourcecode

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

echo [1/3] Compiling Java sources...
powershell -NoProfile -Command "$files = Get-ChildItem -Recurse -Filter *.java | Where-Object { $_.FullName -notmatch '\\out\\' } | Select-Object -ExpandProperty FullName; javac --release 21 --module-path '%FX_LIB%' --add-modules javafx.controls,javafx.fxml -encoding UTF-8 -d '%OUT_DIR%' $files"
if errorlevel 1 (
    echo Compile failed!
    pause
    exit /b 1
)

echo [2/3] Syncing view resources...
xcopy /E /I /Y /Q "%~dp0view" "%OUT_DIR%\view" >nul

echo [3/3] Launching Smart Farm...
java --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.fxml -cp "%OUT_DIR%" Main
endlocal

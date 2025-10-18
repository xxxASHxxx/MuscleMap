@echo off
echo ========================================
echo    MuscleMap Application Launcher
echo ========================================
echo.

echo Building application...
call mvnw clean compile -q

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Starting MuscleMap Application...
echo.

REM Try JavaFX Maven plugin
echo Attempting to run with JavaFX Maven plugin...
call mvnw javafx:run -Djavafx.mainClass=com.musclemmap.MuscleMapApp

if %ERRORLEVEL% neq 0 (
    echo JavaFX Maven plugin failed, trying alternative...
    
    REM Try exec plugin
    call mvnw exec:java -Dexec.mainClass=com.musclemmap.MuscleMapApp
    
    if %ERRORLEVEL% neq 0 (
        echo All Maven methods failed!
        echo Please run the application manually from your IDE.
        pause
    )
)

pause

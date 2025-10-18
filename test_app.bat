@echo off
echo ========================================
echo    MuscleMap Application Test
echo ========================================
echo.

echo Checking if application is running...
tasklist | findstr java > nul
if %ERRORLEVEL% equ 0 (
    echo ✅ Java processes are running - Application appears to be running!
    echo.
    echo The MuscleMap application should be visible on your screen.
    echo.
    echo Features to test:
    echo 1. Login with any credentials
    echo 2. Go to Muscle Map tab
    echo 3. Select a muscle group (e.g., Biceps)
    echo 4. Click on any exercise (e.g., Barbell Curl)
    echo 5. Check if the GIF loads in the Exercise Details section
    echo 6. Verify dropdown text is visible in all tabs
    echo.
    echo If you don't see the application window, try:
    echo - Check your taskbar for the application
    echo - Look for a Java application window
    echo - The application might be running in the background
) else (
    echo ❌ No Java processes found - Application may not be running
    echo.
    echo To start the application manually:
    echo 1. Open your IDE (IntelliJ IDEA)
    echo 2. Run the MuscleMapApp class
    echo 3. Or use: ./mvnw javafx:run
)

echo.
pause

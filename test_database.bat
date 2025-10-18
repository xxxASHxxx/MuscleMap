@echo off
echo ========================================
echo    Database Test Script
echo ========================================
echo.

echo Testing database connection and data...
echo.

echo 1. Checking if database exists...
if exist musclemap.db (
    echo ✅ Database file exists: musclemap.db
) else (
    echo ❌ Database file NOT found: musclemap.db
    echo Please make sure the database file is in the project root.
    pause
    exit /b 1
)

echo.
echo 2. Checking database schema...
sqlite3 musclemap.db "PRAGMA table_info(exercises);"

echo.
echo 3. Checking total exercises...
sqlite3 musclemap.db "SELECT COUNT(*) as total_exercises FROM exercises;"

echo.
echo 4. Checking exercises by muscle group...
sqlite3 musclemap.db "SELECT musclegroup, COUNT(*) as count FROM exercises GROUP BY musclegroup ORDER BY musclegroup;"

echo.
echo 5. Checking GIF URLs...
sqlite3 musclemap.db "SELECT name, gifurl FROM exercises WHERE gifurl IS NOT NULL LIMIT 5;"

echo.
echo 6. Testing specific muscle group (Biceps)...
sqlite3 musclemap.db "SELECT name, equipment, difficulty FROM exercises WHERE musclegroup = 'Biceps';"

echo.
echo ========================================
echo    Database Test Complete
echo ========================================
echo.
echo If you see exercise data above, the database is working correctly.
echo If you see errors or empty results, there might be a database issue.
echo.
pause

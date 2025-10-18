@echo off
echo Starting MuscleMap Application...
echo.

REM Set JavaFX module path
set MODULE_PATH=C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-media\21.0.6\javafx-media-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-media\21.0.6\javafx-media-21.0.6-win.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-web\21.0.6\javafx-web-21.0.6.jar;C:\Users\Ashmit\.m2\repository\org\openjfx\javafx-web\21.0.6\javafx-web-21.0.6-win.jar

REM Set classpath
set CLASSPATH=target\classes;%MODULE_PATH%;C:\Users\Ashmit\.m2\repository\org\xerial\sqlite-jdbc\3.42.0.0\sqlite-jdbc-3.42.0.0.jar;C:\Users\Ashmit\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.17.2\jackson-databind-2.17.2.jar;C:\Users\Ashmit\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.17.2\jackson-annotations-2.17.2.jar;C:\Users\Ashmit\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.17.2\jackson-core-2.17.2.jar

echo Running application...
java -cp "%CLASSPATH%" com.musclemmap.MuscleMapApp

pause

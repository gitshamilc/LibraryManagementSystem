@echo off
echo Building GP6 Library System...
if not exist out mkdir out
javac -cp "lib/*" -d out src\*.java src\core\*.java src\models\*.java src\dao\*.java src\controllers\*.java src\services\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b %errorlevel%
)
echo Compilation successful.

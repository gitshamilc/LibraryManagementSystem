@echo off
title Windows PowerShell
cls
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" tools.DbConsole
echo.
pause

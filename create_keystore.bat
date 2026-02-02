@echo off
REM Create a release keystore for Focus Timer (run once, save the .jks file and passwords forever).
REM Do NOT commit focus-timer-release.jks or your passwords to version control.

set KEYSTORE=focus-timer-release.jks
set ALIAS=focus-timer
set VALIDITY=10000

REM Find keytool: use PATH, then JAVA_HOME, then Android Studio's JBR
set KEYTOOL=
where keytool >nul 2>&1 && set KEYTOOL=keytool
if not defined KEYTOOL if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME%\bin\keytool.exe"
if not defined KEYTOOL if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
if not defined KEYTOOL if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool.exe" set "KEYTOOL=%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool.exe"
if not defined KEYTOOL (
    echo keytool was not found.
    echo.
    echo Try one of these:
    echo   1. Install JDK and add its bin folder to PATH, or set JAVA_HOME to the JDK folder.
    echo   2. If Android Studio is installed, run this from Android Studio's Terminal:
    echo      "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\keytool" -genkey -v -keystore focus-timer-release.jks -alias focus-timer -keyalg RSA -keysize 2048 -validity 10000
    echo   3. Or find keytool.exe in your JDK or Android Studio install and run it with the same -genkey ... arguments.
    pause
    exit /b 1
)

echo Creating keystore: %KEYSTORE%
echo You will be asked for a keystore password and a key password (can be the same).
echo SAVE THESE PASSWORDS AND THE .jks FILE FOREVER - you need them for every Play Store update.
echo.

"%KEYTOOL%" -genkey -v -keystore %KEYSTORE% -alias %ALIAS% -keyalg RSA -keysize 2048 -validity %VALIDITY%

if %ERRORLEVEL% equ 0 (
    echo.
    echo Keystore created: %KEYSTORE%
    echo 1. Move or copy %KEYSTORE% to a safe place and back it up.
    echo 2. Copy keystore.properties.example to keystore.properties
    echo 3. Edit keystore.properties with your storePassword, keyPassword, keyAlias=%ALIAS%, and storeFile path to %KEYSTORE%
    echo 4. Never commit keystore.properties or %KEYSTORE% to git.
) else (
    echo keytool failed.
)
pause

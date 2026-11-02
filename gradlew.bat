@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
goto init

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto init
echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:init
@rem Get command-line arguments
set CMD_LINE_ARGS=
set _SKIP=2
:win9xME_args_slurp
if "x%~1" == "x" goto execute
set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
@rem Execute Gradle
"%JAVA_EXE%" -Xmx64m -Xms64m -Dorg.gradle.appname="%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
if "%ERRORLEVEL%"=="0" goto mainEnd
:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe return code!
if not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1
:mainEnd
if "%OS%"=="Windows_NT" endlocal
:omega

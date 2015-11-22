@echo off

SETLOCAL

set JAVA=javaw
if defined JAVA_HOME set JAVA=%JAVA_HOME%\bin\%JAVA%
REM Replace Program Files
set JAVA=%JAVA:Program Files=PROGRA~1%

REM JAVA options
REM You can set JVM additional options here if you want
if NOT DEFINED JVM_OPTS set JVM_OPTS=-server -Xms128m -Xmx512m -XX:MetaspaceSize=250m -XX:MaxMetaspaceSize=250m -XX:ReservedCodeCacheSize=150m -XX:+UseConcMarkSweepGC -XX:SoftRefLRUPolicyMSPerMB=50 -ea -Dsun.io.useCanonCaches=false -Djava.net.preferIPv4Stack=true -Dawt.useSystemAAFontSettings=lcd -XX:+TieredCompilation
REM Set up security options
REM set SECURITY_OPTS=-Djava.security.debug=failure -Djava.security.manager"
set SECURITY_OPTS=-Djava.security.debug=failure
REM Combined java options
set JAVA_OPTS=%SECURITY_OPTS% %JAVA_OPTS% %JVM_OPTS%

set PRG_DIR=%~dp0

echo Starting ganesa
cd "%PRG_DIR%"
start %JAVA% %JAVA_OPTS% -jar lib\ganesa.jar %*
goto finally

:finally
ENDLOCAL

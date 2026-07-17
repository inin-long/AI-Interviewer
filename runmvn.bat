@echo off
set JAVA_HOME=C:\Users\Administrator\.workbuddy\binaries\jdk21\jdk-21.0.11+10
set MAVEN_OPTS=
call C:\Users\Administrator\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd %* >> C:\Users\Administrator\AppData\Local\Temp\build.log 2>&1
echo MVN_DONE_EXIT=%ERRORLEVEL% >> C:\Users\Administrator\AppData\Local\Temp\build.log

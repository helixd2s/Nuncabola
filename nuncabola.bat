@echo off
setlocal
set PATH=%PATH%;%SystemRoot%\SysWOW64
start "" javaw.exe -XX:+UseConcMarkSweepGC -jar nuncabola.jar
endlocal

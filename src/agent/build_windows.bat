echo off
cd /d %~dp0

set WORK_DIR=%CD%
set GOPATH=%WORK_DIR%

set GOARCH=386
set GOOS=windows

cd /D %WORK_DIR%
md %WORK_DIR%\bin
del /q %WORK_DIR%\bin\*

cd %WORK_DIR%\src\cmd\daemon
go build -o %WORK_DIR%\bin\devopsDaemon.exe

cd %WORK_DIR%\src\cmd\agent
go build -o %WORK_DIR%\bin\devopsAgent.exe

cd %WORK_DIR%\src\cmd\upgrader
go build -o %WORK_DIR%\bin\upgrader.exe

cd /D %WORK_DIR%

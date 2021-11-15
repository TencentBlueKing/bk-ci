echo off
cd /d %~dp0

set WORK_DIR=%CD%

set GOARCH=386
set GOOS=windows

cd /D %WORK_DIR%
md %WORK_DIR%\bin
del /q %WORK_DIR%\bin\*

set GO111MODULE=on

go build -o %WORK_DIR%\bin\devopsDaemon.exe %WORK_DIR%\src\cmd\daemon

go build -o %WORK_DIR%\bin\devopsAgent.exe %WORK_DIR%\src\cmd\agent

go build -o %WORK_DIR%\bin\upgrader.exe %WORK_DIR%\src\cmd\upgrader

go build -o %WORK_DIR%\bin\installer.exe %WORK_DIR%\src\cmd\installer


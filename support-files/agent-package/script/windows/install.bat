@echo off

echo start install devops agent...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

echo work_dir %work_dir%

IF EXIST jre (
    echo "jre already exists, skip unzip"
) else (
    echo "unzip jre"
    unzip -o jre.zip -d jre
)

mkdir logs
mkdir workspace

schtasks /query | findstr %service_name%

IF ERRORLEVEL 1 GOTO :use_register_service
GOTO :start_schtasks

:use_register_service
sc query %service_name%
IF ERRORLEVEL 1 GOTO :service_create
GOTO :service_start

:service_create
echo install agent service
sc create %service_name% binPath= "%work_dir%\devopsDaemon.exe" start= auto
GOTO :service_start

:service_start
echo start agent service
sc start %service_name%
GOTO :finally_label

:start_schtasks
echo start devops daemon by devopsctl.vbs
devopsctl.vbs
GOTO :finally_label

:finally_label
pause

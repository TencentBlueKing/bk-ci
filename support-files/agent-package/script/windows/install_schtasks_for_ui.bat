@echo off

echo start install devops daemon...
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

sc query %service_name%

IF ERRORLEVEL 1 GOTO :create_schtasks

GOTO :remove_service

:remove_service

echo stop devops daemon service
sc stop %service_name%
echo uninstall devops daemon service
sc delete %service_name%

GOTO :create_schtasks

:create_schtasks

echo run %service_name% when any one logon
schtasks /create /tn %service_name% /tr "cscript  %~dp0\devopsctl.vbs" /sc ONLOGON /F

GOTO :start_process

:start_process
echo start devops daemon first
devopsctl.vbs

echo "done"
pause

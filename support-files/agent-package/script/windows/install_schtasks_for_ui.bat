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

echo install %service_name% task schedule

schtasks /create /tn %service_name% /tr "cscript  %~dp0\devopsctl.vbs" /sc ONLOGON /F


echo start devops daemon
devopsctl.vbs

echo "done"
pause

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

echo install agent service
sc create %service_name% binPath= "%work_dir%\devopsDaemon.exe" start= auto

echo start agent service
sc start %service_name%

pause
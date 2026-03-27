@echo off

echo start uninstall devops daemon schtasks...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

sc query %service_name%

if ERRORLEVEL 1 GOTO :delete_schtasks
GOTO :delete_service

:delete_service
echo stop devops daemon service
sc stop %service_name%
echo uninstall devops daemon service
sc delete %service_name%

GOTO delete_schtasks

:delete_schtasks

schtasks /delete /tn %service_name% /F
GOTO :kill_process

:kill_process
set /p daemonPid=<runtime\daemon.pid
echo "kill devopsDaemon.exe process %daemonPid%"
tskill %daemonPid%

set /p agentPid=<runtime\agent.pid
echo "kill devopsAgent.exe process %agentPid%"
tskill %agentPid%

echo "uninstall devops daemon schtasks done"

pause

@echo off

cd /d %~dp0
set workspace=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

schtasks /query | findstr %service_name%

IF ERRORLEVEL 1 GOTO :stop_service
GOTO :stop_schtasks

:stop_schtasks
set /p daemonPid=<runtime\daemon.pid
echo "kill devopsDaemon.exe process %daemonPid%"
tskill %daemonPid%

set /p agentPid=<runtime\agent.pid
echo "kill devopsAgent.exe process %agentPid%"
tskill %agentPid%

GOTO :stop_service


:stop_service
sc query %service_name%

IF ERRORLEVEL 1 GOTO :finally_end
GOTO :stop_service

:stop_service
echo "stop devops service"
sc stop %service_name%
GOTO :finally_end

:finally_end
pause

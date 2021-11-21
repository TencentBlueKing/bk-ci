@echo off

echo start uninstall devops agent...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

schtasks /delete /tn %service_name% /F

set /p daemonPid=<runtime\daemon.pid
echo "kill devopsDaemon process %daemonPid%"
tskill %daemonPid%

set /p agentPid=<runtime\agent.pid
echo "kill devopsAgent process %agentPid%"
tskill %agentPid%

echo "done"

pause

@echo off

cd /d %~dp0
set workspace=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

schtasks /query | findstr %service_name%

IF ERRORLEVEL 1 GOTO :start_service
GOTO :start_schtasks

:start_schtasks
echo start devops daemon by devopsctl.vbs
devopsctl.vbs
GOTO :finally_end

:start_service
echo start devops daemon by service
sc start %service_name%
GOTO :finally_end

:finally_end
pause

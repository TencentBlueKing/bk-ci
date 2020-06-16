@echo off

cd /d %~dp0
set workspace=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

sc start %service_name%

pause
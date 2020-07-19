@echo off

set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

echo stop agent service
sc stop %service_name%

echo uninstall agent service
sc delete %service_name%

pause

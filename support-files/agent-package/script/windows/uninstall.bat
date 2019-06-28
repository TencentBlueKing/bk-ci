@echo off

set service_name=landun_devops_agent

echo stop agent service
sc stop %service_name%

echo uninstall agent service
sc delete %service_name%

pause

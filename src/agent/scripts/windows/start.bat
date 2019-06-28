@echo off

cd /d %~dp0
set workspace=%CD%
set service_name=landun_devops_agent

sc start %service_name%

pause
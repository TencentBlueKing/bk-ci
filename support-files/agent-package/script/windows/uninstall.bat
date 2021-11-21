@echo off

set agent_id=dnmrrvme
set service_name=devops_agent_%agent_id%

call uninstall_schtasks_for_ui.bat

pause

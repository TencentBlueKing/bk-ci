@echo off

echo start uninstall devops daemon...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

REM === Clean up session mode (auto-logon + LSA secrets) ===
IF EXIST "%~dp0configure_session.ps1" (
    echo clean up session mode via configure_session.ps1 -Disable ...
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0configure_session.ps1" -Disable 2>nul
) ELSE (
    echo clean up session mode registry...
    reg add "HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon" /v AutoAdminLogon /t REG_SZ /d 0 /f 2>nul
    reg delete "HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon" /v DefaultPassword /f 2>nul
)

REM === Stop and remove service ===
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

echo "uninstall devops daemon done"

pause

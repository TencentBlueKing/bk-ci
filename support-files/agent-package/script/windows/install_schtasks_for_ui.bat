@echo off

REM 用[计划任务]管理。 当用户登录时，自动启动devopsDaemon.exe，
REM 与install.bat的win服务注册方式相比：
REM 优点: 能够显示出在构建过程中拉起UI界面的程序。
REM 缺点：不支持开机即启动，需要帐号登录后才可以。

echo start install devops daemon...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

echo work_dir %work_dir%

IF EXIST jre (
    echo "jre already exists, skip unzip"
) else (
    echo "unzip jre"
    unzip -o jre.zip -d jre
)

mkdir logs
mkdir workspace

REM 如果查到之前已经使用install.bat脚本注册到win服务，则先进行停服并删除，再将任务添加到「计划任务」中
sc query %service_name%

IF ERRORLEVEL 1 GOTO :create_schtasks

GOTO :remove_service

:remove_service

echo stop devops daemon service
sc stop %service_name%
echo uninstall devops daemon service
sc delete %service_name%

GOTO :create_schtasks

:create_schtasks

echo run %service_name% when any one logon
schtasks /create /tn %service_name% /tr "cscript  %~dp0\devopsctl.vbs" /sc ONLOGON /F

GOTO :start_process

:start_process
echo start devops daemon first
devopsctl.vbs

echo "done"
pause

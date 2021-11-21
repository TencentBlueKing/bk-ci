@echo off

REM win服务注册的方式，优点：系统开机即自动启动，不需要用户登录
REM 缺点：构建过程中拉起带UI的程序不可见
echo start install devops agent...
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

echo install agent service
sc create %service_name% binPath= "%work_dir%\devopsDaemon.exe" start= auto

echo start agent service
sc start %service_name%

pause

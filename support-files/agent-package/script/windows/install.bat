@echo off

cd /d %~dp0
set work_dir=%CD%
set service_name=landun_devops_agent
echo work_dir %work_dir%

echo "unzip jre"
unzip -o jre.zip -d jre

mkdir logs
mkdir workspace

echo install agent service
REM editrights -a SeServiceLogonRight -u "xxxx"
sc create %service_name% binPath= "%work_dir%\devopsDaemon.exe" start= auto
REM sc create %service_name% binPath= %work_dir%\devopsDaemon.exe start= auto obj= "TENCENT\xxxx" password= "xxxx"

echo start agent service
sc start %service_name%

pause
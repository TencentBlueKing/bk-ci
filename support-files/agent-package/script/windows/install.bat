@echo off

echo start install devops agent...
cd /d %~dp0
set work_dir=%CD%
set agent_id=##agentId##
set service_name=devops_agent_%agent_id%

echo work_dir %work_dir%

IF EXIST jdk (
    echo "jdk already exists, skip unzip"
) else (
    echo "unzip jdk"
    Call :UnZipFile "%~dp0jdk\" "%~dp0jre.zip"
)

mkdir logs
mkdir workspace

schtasks /query | findstr %service_name%

IF ERRORLEVEL 1 GOTO :use_register_service
GOTO :start_schtasks

:use_register_service
sc query %service_name%
IF ERRORLEVEL 1 GOTO :service_create
GOTO :service_start

:service_create
echo install agent service
sc create %service_name% binPath= "%work_dir%\devopsDaemon.exe" start= auto
GOTO :service_start

:service_start
echo start agent service
sc start %service_name%
GOTO :finally_label

:start_schtasks
echo start devops daemon by devopsctl.vbs
devopsctl.vbs
GOTO :finally_label

:finally_label
pause
exit /b

:UnZipFile <ExtractTo> <newzipfile>
set vbs="%temp%\_.vbs"
if exist %vbs% del /f /q %vbs%
>%vbs%  echo Set fso = CreateObject("Scripting.FileSystemObject")
>>%vbs% echo If NOT fso.FolderExists(%1) Then
>>%vbs% echo fso.CreateFolder(%1)
>>%vbs% echo End If
>>%vbs% echo set objShell = CreateObject("Shell.Application")
>>%vbs% echo set FilesInZip=objShell.NameSpace(%2).items
>>%vbs% echo objShell.NameSpace(%1).CopyHere(FilesInZip)
>>%vbs% echo Set fso = Nothing
>>%vbs% echo Set objShell = Nothing
cscript //nologo %vbs%
if exist %vbs% del /f /q %vbs%

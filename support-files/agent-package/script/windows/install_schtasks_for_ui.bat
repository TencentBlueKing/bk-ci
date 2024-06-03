@echo off

echo start install devops daemon...
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

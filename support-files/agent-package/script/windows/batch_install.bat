@echo off

:: for batch install for other computer
echo start new install devops agent...
cd /d %~dp0
set work_dir=%CD%

echo work_dir %work_dir%

tmp\installer -action install

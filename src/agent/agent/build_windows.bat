echo off
cd /d %~dp0
Powershell.exe -executionpolicy remotesigned -File .\build_windows.ps1

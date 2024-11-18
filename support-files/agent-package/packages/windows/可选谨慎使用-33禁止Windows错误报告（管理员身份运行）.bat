# 该功能可以大幅度降低app crash的等待时间，它免除了windows收集app crash报告和生成dmp的时间。如果用户不需要通过这种方式去debug错误，可以使用。
sc config WerSvc start=disabled
net stop WerSvc
reg delete "HKLM\SOFTWARE\Microsoft\Windows\Windows Error Reporting" /v Disabled /f
reg delete "HKCU\Software\Microsoft\Windows\Windows Error Reporting" /v Disabled /f
reg add "HKLM\SOFTWARE\Microsoft\Windows\Windows Error Reporting" /v Disabled /t REG_DWORD /d 1 /f
reg add "HKCU\Software\Microsoft\Windows\Windows Error Reporting" /v Disabled /t REG_DWORD /d 1 /f
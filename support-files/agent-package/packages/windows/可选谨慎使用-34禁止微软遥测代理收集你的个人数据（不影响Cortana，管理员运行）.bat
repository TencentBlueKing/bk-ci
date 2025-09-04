# 它可以干掉微软尝试在你的系统上收集你使用数据的能力，解决莫名其妙compattelrunner大量吃CPU问题 
takeown /f "%windir%\system32\compattelrunner.exe" /a
icacls "%windir%\system32\compattelrunner.exe" /c /deny everyone:(r,rx,rd,rea)
icacls "%windir%\system32\compattelrunner.exe" /c /grant administrators:(f)
icacls "%windir%\system32\compattelrunner.exe" /c /setowner "nt service\trustedinstaller"
sc config diagtrack start=disabled
sc config dmwappushservice start=disabled
reg delete "HKLM\SOFTWARE\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\DataCollection" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKCU\Software\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /f
reg add "HKCU\Software\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKLM\SOFTWARE\Microsoft\DataCollection" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\Microsoft\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKLM\SOFTWARE\Microsoft\Windows\PrivacySettingsBeforeCreatorsUpdate\DataCollectionLevel" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\Microsoft\Windows\PrivacySettingsBeforeCreatorsUpdate\DataCollectionLevel" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKLM\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Policies\DataCollection" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Policies\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
reg delete "HKLM\SOFTWARE\WOW6432Node\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /f
reg add "HKLM\SOFTWARE\WOW6432Node\Policies\Microsoft\Windows\DataCollection" /v AllowTelemetry /t REG_DWORD /d 0 /f
schtasks /Change /TN "\Microsoft\Windows\Application Experience\Microsoft Compatibility Appraiser" /DISABLE
schtasks /Change /TN "\Microsoft\Windows\Application Experience\ProgramDataUpdater" /DISABLE
schtasks /Change /TN "\Microsoft\Windows\Application Experience\AITAgent" /DISABLE
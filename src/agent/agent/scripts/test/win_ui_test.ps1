$ErrorActionPreference = "Stop"
$failed = $false

Write-Host "============================================"
Write-Host "  BK-CI Windows Session Capability Test"
Write-Host "============================================"
Write-Host ""

# ── 1. Session ID 检测 ──
# Session 0 是服务会话，用户桌面会话 ID > 0
$proc = Get-Process -Id $PID
$sessionId = $proc.SessionId
Write-Host "[Check 1] Session ID: $sessionId"
if ($sessionId -eq 0) {
    Write-Host "  FAIL: Running in Session 0 (service isolation session)" -ForegroundColor Red
    $failed = $true
} else {
    Write-Host "  PASS: Running in user session (ID > 0)" -ForegroundColor Green
}
Write-Host ""

# ── 2. 运行用户检测 ──
# 确认不是 SYSTEM 身份
$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$userName = $identity.Name
Write-Host "[Check 2] Current user: $userName"
if ($userName -match "SYSTEM") {
    Write-Host "  FAIL: Running as NT AUTHORITY\SYSTEM" -ForegroundColor Red
    $failed = $true
} else {
    Write-Host "  PASS: Running as interactive user" -ForegroundColor Green
}
Write-Host ""

# ── 3. 桌面对象可访问性 ──
# 检查能否打开 winsta0\default 桌面（Session 0 中无法访问用户桌面）
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class DesktopCheck {
    [DllImport("user32.dll", SetLastError = true)]
    public static extern IntPtr OpenDesktop(string lpszDesktop, uint dwFlags, bool fInherit, uint dwDesiredAccess);
    [DllImport("user32.dll")]
    public static extern bool CloseDesktop(IntPtr hDesktop);
    [DllImport("user32.dll")]
    public static extern IntPtr GetThreadDesktop(uint dwThreadId);
    [DllImport("kernel32.dll")]
    public static extern uint GetCurrentThreadId();

    public const uint DESKTOP_READOBJECTS = 0x0001;
    public const uint DESKTOP_CREATEWINDOW = 0x0002;
}
"@

$hDesktop = [DesktopCheck]::OpenDesktop("Default", 0, $false, 0x0003)
Write-Host "[Check 3] Desktop 'Default' handle: $hDesktop"
if ($hDesktop -eq [IntPtr]::Zero) {
    Write-Host "  FAIL: Cannot open 'Default' desktop" -ForegroundColor Red
    $failed = $true
} else {
    [DesktopCheck]::CloseDesktop($hDesktop) | Out-Null
    Write-Host "  PASS: Desktop accessible with READOBJECTS | CREATEWINDOW" -ForegroundColor Green
}
Write-Host ""

# ── 4. 显示器/屏幕枚举 ──
# 用户会话应该能检测到至少一个显示器
Add-Type -AssemblyName System.Windows.Forms
$screens = [System.Windows.Forms.Screen]::AllScreens
Write-Host "[Check 4] Display count: $($screens.Count)"
foreach ($s in $screens) {
    Write-Host "  - $($s.DeviceName): $($s.Bounds.Width)x$($s.Bounds.Height), Primary=$($s.Primary)"
}
if ($screens.Count -eq 0) {
    Write-Host "  FAIL: No display detected" -ForegroundColor Red
    $failed = $true
} else {
    Write-Host "  PASS: Display(s) available" -ForegroundColor Green
}
Write-Host ""

# ── 5. UI 窗口创建测试 ──
# 创建一个 WinForms 窗口，显示后关闭，验证 UI 子系统正常
Write-Host "[Check 5] Creating test window..."
try {
    $form = New-Object System.Windows.Forms.Form
    $form.Text = "BK-CI Session Test"
    $form.Size = New-Object System.Drawing.Size(300, 200)
    $form.StartPosition = "CenterScreen"
    $form.TopMost = $true
    $form.Show()
    $form.Refresh()
    Start-Sleep -Milliseconds 800
    $form.Close()
    $form.Dispose()
    Write-Host "  PASS: Window created, shown, and closed successfully" -ForegroundColor Green
} catch {
    Write-Host "  FAIL: Window creation failed: $_" -ForegroundColor Red
    $failed = $true
}
Write-Host ""

# ── 6. 截图测试 ──
# 能截到屏幕内容说明有真实的桌面渲染
Write-Host "[Check 6] Taking screenshot..."
try {
    Add-Type -AssemblyName System.Drawing
    $primary = [System.Windows.Forms.Screen]::PrimaryScreen
    $w = $primary.Bounds.Width
    $h = $primary.Bounds.Height
    $bitmap = New-Object System.Drawing.Bitmap($w, $h)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.CopyFromScreen(0, 0, 0, 0, (New-Object System.Drawing.Size($w, $h)))
    $graphics.Dispose()

    $screenshotPath = Join-Path $env:WORKSPACE "session_test_screenshot.png"
    if (-not $screenshotPath -or $screenshotPath -eq "session_test_screenshot.png") {
        $screenshotPath = Join-Path $PWD "session_test_screenshot.png"
    }
    $bitmap.Save($screenshotPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    $fileSize = (Get-Item $screenshotPath).Length
    Write-Host "  PASS: Screenshot saved ($fileSize bytes): $screenshotPath" -ForegroundColor Green
} catch {
    Write-Host "  WARN: Screenshot failed (may still be in session): $_" -ForegroundColor Yellow
}
Write-Host ""

# ── 7. 环境变量检查 ──
# Session 模式下 agent 应该继承用户环境
Write-Host "[Check 7] Key environment variables:"
Write-Host "  SESSIONNAME  = $env:SESSIONNAME"
Write-Host "  USERNAME     = $env:USERNAME"
Write-Host "  USERDOMAIN   = $env:USERDOMAIN"
Write-Host "  USERPROFILE  = $env:USERPROFILE"
Write-Host "  DEVOPS_AGENT_WIN_SERVICE = $env:DEVOPS_AGENT_WIN_SERVICE"
Write-Host ""

# ── 汇总 ──
Write-Host "============================================"
if ($failed) {
    Write-Host "  RESULT: FAILED - Not running in proper user session" -ForegroundColor Red
    Write-Host "============================================"
    exit 1
} else {
    Write-Host "  RESULT: ALL PASSED - Session mode is working correctly" -ForegroundColor Green
    Write-Host "============================================"
    exit 0
}
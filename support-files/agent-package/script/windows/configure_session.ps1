#Requires -RunAsAdministrator
<#
.SYNOPSIS
    One-step install / configure BK-CI Agent in Session mode (desktop-interactive).

.DESCRIPTION
    This script is the session-mode counterpart of install.bat.

    Typical workflows:

      Fresh install    :  download_install.ps1  ->  (already running as SERVICE)
      Switch to session:  configure_session.ps1 -UserName ... -Password ...
      Uninstall all    :  uninstall.bat
      Reinstall session:  configure_session.ps1 -UserName ... -Password ...
      Revert to plain  :  configure_session.ps1 -Disable   (then use install.bat if needed)

    The script is idempotent — safe to run multiple times. It handles:
      - JDK unzipping and directory creation (same as install.bat)
      - Cleaning up existing schtasks / service before creating the service
      - Registering the daemon as a Windows service (auto start)
      - Configuring Windows Auto-Logon (password via LSA Secret)
      - Creating an auto-lock scheduled task for security
      - Restarting the daemon service
      - Prompting to reboot for auto-logon activation

    Modeled after Azure DevOps Agent --runAsAutoLogon.

.PARAMETER UserName
    Windows logon account (e.g. "builduser", "DOMAIN\builduser", "user@domain").

.PARAMETER Password
    Password for the logon account.

.PARAMETER NoLock
    Skip auto-lock after logon. By default the workstation is locked 5 seconds
    after auto-logon for security.

.PARAMETER NoRestart
    Do not prompt to reboot after configuration.

.PARAMETER Disable
    Revert to plain service mode: remove auto-logon and auto-lock settings,
    restart the daemon service. The service itself is kept.

.EXAMPLE
    # Full install in session mode (after download_install or after uninstall)
    .\configure_session.ps1 -UserName builduser -Password P@ssw0rd

    # Session mode without auto-lock, no reboot prompt
    .\configure_session.ps1 -UserName builduser -Password P@ssw0rd -NoLock -NoRestart

    # Revert to plain service mode (keep the service, remove auto-logon)
    .\configure_session.ps1 -Disable
#>

[CmdletBinding(DefaultParameterSetName = "Enable")]
param(
    [Parameter(ParameterSetName = "Enable", Mandatory = $true)]
    [string]$UserName,

    [Parameter(ParameterSetName = "Enable", Mandatory = $true)]
    [string]$Password,

    [Parameter(ParameterSetName = "Enable")]
    [switch]$NoLock,

    [Parameter(ParameterSetName = "Enable")]
    [switch]$NoRestart,

    [Parameter(ParameterSetName = "Disable", Mandatory = $true)]
    [switch]$Disable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:WorkDir       = Split-Path -Parent $MyInvocation.MyCommand.Definition
$script:WinlogonPath  = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon"
$script:LockTaskName  = "BkCiAgentAutoLock"
$script:PropsFile     = Join-Path $WorkDir ".agent.properties"

# ═══════════════════════════════════════════════════════════════════════════
# LSA Secret helper — same P/Invoke as Sysinternals Autologon
# ═══════════════════════════════════════════════════════════════════════════
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class LsaSecret
{
    [StructLayout(LayoutKind.Sequential)]
    private struct LSA_UNICODE_STRING
    {
        public ushort Length;
        public ushort MaximumLength;
        public IntPtr Buffer;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct LSA_OBJECT_ATTRIBUTES
    {
        public int    Length;
        public IntPtr RootDirectory;
        public IntPtr ObjectName;
        public uint   Attributes;
        public IntPtr SecurityDescriptor;
        public IntPtr SecurityQualityOfService;
    }

    [DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
    private static extern uint LsaOpenPolicy(
        ref LSA_UNICODE_STRING    SystemName,
        ref LSA_OBJECT_ATTRIBUTES ObjectAttributes,
        uint DesiredAccess, out IntPtr PolicyHandle);

    [DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
    private static extern uint LsaStorePrivateData(
        IntPtr PolicyHandle,
        ref LSA_UNICODE_STRING KeyName,
        ref LSA_UNICODE_STRING PrivateData);

    [DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
    private static extern uint LsaStorePrivateData(
        IntPtr PolicyHandle,
        ref LSA_UNICODE_STRING KeyName,
        IntPtr PrivateData);

    [DllImport("advapi32.dll", SetLastError = true, PreserveSig = true)]
    private static extern uint LsaClose(IntPtr PolicyHandle);

    [DllImport("advapi32.dll")]
    private static extern int LsaNtStatusToWinError(uint status);

    private const uint POLICY_CREATE_SECRET = 0x00000020;

    private static LSA_UNICODE_STRING ToLsaString(string s)
    {
        var lsa = new LSA_UNICODE_STRING();
        lsa.Buffer        = Marshal.StringToHGlobalUni(s);
        lsa.Length         = (ushort)(s.Length * 2);
        lsa.MaximumLength  = (ushort)((s.Length + 1) * 2);
        return lsa;
    }

    private static IntPtr OpenPolicy()
    {
        var attrs  = new LSA_OBJECT_ATTRIBUTES();
        attrs.Length = Marshal.SizeOf(attrs);
        var system = new LSA_UNICODE_STRING();
        IntPtr handle;
        uint status = LsaOpenPolicy(ref system, ref attrs,
                                    POLICY_CREATE_SECRET, out handle);
        if (status != 0)
            throw new Exception("LsaOpenPolicy failed, Win32 error " +
                                LsaNtStatusToWinError(status));
        return handle;
    }

    public static void Store(string key, string secret)
    {
        IntPtr handle = OpenPolicy();
        try
        {
            var keyStr    = ToLsaString(key);
            var secretStr = ToLsaString(secret);
            uint status = LsaStorePrivateData(handle, ref keyStr, ref secretStr);
            if (status != 0)
                throw new Exception("LsaStorePrivateData failed, Win32 error " +
                                    LsaNtStatusToWinError(status));
        }
        finally { LsaClose(handle); }
    }

    public static void Delete(string key)
    {
        IntPtr handle = OpenPolicy();
        try
        {
            var keyStr = ToLsaString(key);
            LsaStorePrivateData(handle, ref keyStr, IntPtr.Zero);
        }
        finally { LsaClose(handle); }
    }
}
"@ -Language CSharp

# ═══════════════════════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════════════════════
function Write-Step { param([string]$Msg) Write-Host "[BK-CI] $Msg" }
function Write-Warn { param([string]$Msg) Write-Host "[BK-CI][WARN] $Msg" -ForegroundColor Yellow }

function Get-AgentId {
    if (-not (Test-Path $PropsFile)) {
        throw ".agent.properties not found at $PropsFile"
    }
    $match = Select-String -Path $PropsFile -Pattern "^devops\.agent\.id=(.+)$"
    if (-not $match) { throw "devops.agent.id not found in $PropsFile" }
    return $match.Matches[0].Groups[1].Value.Trim()
}

function Get-ServiceName {
    return "devops_agent_$(Get-AgentId)"
}

function Split-UserDomain {
    param([string]$Account)
    if ($Account.Contains("\")) {
        $parts = $Account.Split("\", 2)
        return @{ Domain = $parts[0]; User = $parts[1] }
    }
    if ($Account.Contains("@")) {
        $parts = $Account.Split("@", 2)
        return @{ Domain = $parts[1]; User = $parts[0] }
    }
    return @{ Domain = $env:COMPUTERNAME; User = $Account }
}

function Test-ServiceExists {
    param([string]$Name)
    $null = & sc.exe query $Name 2>$null
    return ($LASTEXITCODE -eq 0)
}

function Test-ServiceRunning {
    param([string]$Name)
    $out = & sc.exe query $Name 2>&1
    return ($out -match "RUNNING")
}

function Stop-DaemonSafe {
    param([string]$ServiceName)

    # Stop service if running
    if ((Test-ServiceExists $ServiceName) -and (Test-ServiceRunning $ServiceName)) {
        Write-Step "Stopping service $ServiceName ..."
        & sc.exe stop $ServiceName 2>$null | Out-Null
        for ($i = 0; $i -lt 20; $i++) {
            Start-Sleep -Seconds 1
            if (-not (Test-ServiceRunning $ServiceName)) { break }
        }
    }

    # Kill residual processes from schtasks / manual launch
    foreach ($pidFile in @("runtime\daemon.pid", "runtime\agent.pid")) {
        $path = Join-Path $WorkDir $pidFile
        if (Test-Path $path) {
            $pidVal = (Get-Content $path -ErrorAction SilentlyContinue).Trim()
            if ($pidVal) {
                $proc = Get-Process -Id ([int]$pidVal) -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Step "Killing residual process PID $pidVal ($($proc.Name))"
                    Stop-Process -Id ([int]$pidVal) -Force -ErrorAction SilentlyContinue
                }
            }
        }
    }
}

function Unzip-IfNeeded {
    param([string]$ZipFile, [string]$DestDir)
    $zipPath = Join-Path $WorkDir $ZipFile
    $destPath = Join-Path $WorkDir $DestDir
    if (-not (Test-Path $zipPath)) {
        Write-Warn "$ZipFile not found, skipped"
        return
    }
    if (Test-Path $destPath) {
        Write-Step "$DestDir already exists, skipped unzip"
        return
    }
    Write-Step "Unzipping $ZipFile -> $DestDir"
    if ($PSVersionTable.PSVersion.Major -ge 5) {
        Expand-Archive -Path $zipPath -DestinationPath $destPath -Force
    } else {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [System.IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $destPath)
    }
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 1 — Prepare environment (same as install.bat)
# ═══════════════════════════════════════════════════════════════════════════
function Initialize-AgentDir {
    Write-Step "Preparing agent directory..."
    Unzip-IfNeeded "jdk17.zip" "jdk17"
    Unzip-IfNeeded "jre.zip"   "jdk"
    New-Item -ItemType Directory -Force -Path (Join-Path $WorkDir "logs")      | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $WorkDir "workspace") | Out-Null
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 2 — Ensure daemon runs as Windows service
# ═══════════════════════════════════════════════════════════════════════════
function Ensure-DaemonService {
    $serviceName = Get-ServiceName
    $daemonExe   = Join-Path $WorkDir "devopsDaemon.exe"

    if (-not (Test-Path $daemonExe)) {
        throw "devopsDaemon.exe not found at $daemonExe"
    }

    # Stop everything first
    Stop-DaemonSafe -ServiceName $serviceName

    # Remove legacy schtasks if present
    $null = schtasks /query /tn $serviceName 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Step "Removing legacy scheduled task: $serviceName"
        schtasks /delete /tn $serviceName /f 2>$null | Out-Null
    }

    # Delete existing service to ensure clean binPath
    if (Test-ServiceExists $serviceName) {
        Write-Step "Removing existing service to re-create with correct binPath"
        & sc.exe delete $serviceName 2>$null | Out-Null
        Start-Sleep -Seconds 2
    }

    # Create service
    $binPath = "`"$daemonExe`""
    Write-Step "Creating service: $serviceName"
    & sc.exe create $serviceName binPath= $binPath start= auto | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "sc.exe create failed (exit $LASTEXITCODE)"
    }
    Write-Step "Service created: $serviceName (auto start)"
    return $serviceName
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 3 — Configure Auto-Logon
# ═══════════════════════════════════════════════════════════════════════════
function Enable-AutoLogon {
    param([string]$Account, [string]$Pass, [bool]$SetupLock)

    $parsed = Split-UserDomain $Account
    Write-Step "Configuring auto-logon: user=$($parsed.User), domain=$($parsed.Domain)"

    # Winlogon registry
    Set-ItemProperty -Path $WinlogonPath -Name "AutoAdminLogon"    -Value "1"
    Set-ItemProperty -Path $WinlogonPath -Name "DefaultUserName"   -Value $parsed.User
    Set-ItemProperty -Path $WinlogonPath -Name "DefaultDomainName" -Value $parsed.Domain
    Remove-ItemProperty -Path $WinlogonPath -Name "DefaultPassword" -ErrorAction SilentlyContinue
    Remove-ItemProperty -Path $WinlogonPath -Name "AutoLogonCount"  -ErrorAction SilentlyContinue

    # LSA Secret (encrypted password storage)
    [LsaSecret]::Store("DefaultPassword", $Pass)
    Write-Step "Password stored via LSA Secret (encrypted)"

    # Disable lock-screen-on-wake policy
    $powerCfg = "HKLM:\SOFTWARE\Policies\Microsoft\Power\PowerSettings\0e796bdb-100d-47d6-a2d5-f7d2daa51f51"
    if (-not (Test-Path $powerCfg)) { New-Item -Path $powerCfg -Force | Out-Null }
    Set-ItemProperty -Path $powerCfg -Name "DCSettingIndex" -Value 0 -Type DWord
    Set-ItemProperty -Path $powerCfg -Name "ACSettingIndex" -Value 0 -Type DWord

    # Auto-lock after logon
    if ($SetupLock) {
        $null = schtasks /query /tn $LockTaskName 2>$null
        if ($LASTEXITCODE -eq 0) {
            schtasks /delete /tn $LockTaskName /f 2>$null | Out-Null
        }
        schtasks /create `
            /tn $LockTaskName `
            /tr "rundll32.exe user32.dll,LockWorkStation" `
            /sc ONLOGON `
            /delay 0000:05 `
            /rl HIGHEST `
            /f | Out-Null
        Write-Step "Auto-lock scheduled: screen locks 5s after logon"
    }
}

function Disable-AutoLogon {
    Write-Step "Removing auto-logon configuration..."
    Set-ItemProperty -Path $WinlogonPath -Name "AutoAdminLogon" -Value "0"
    Remove-ItemProperty -Path $WinlogonPath -Name "DefaultPassword" -ErrorAction SilentlyContinue
    try { [LsaSecret]::Delete("DefaultPassword") } catch {}
    Write-Step "Auto-logon disabled"

    $null = schtasks /query /tn $LockTaskName 2>$null
    if ($LASTEXITCODE -eq 0) {
        schtasks /delete /tn $LockTaskName /f 2>$null | Out-Null
        Write-Step "Auto-lock task removed"
    }
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 4 — Start service
# ═══════════════════════════════════════════════════════════════════════════
function Start-DaemonService {
    param([string]$Name)
    if (-not (Test-ServiceExists $Name)) {
        Write-Warn "Service $Name not found, skipping start"
        return
    }
    Write-Step "Starting service $Name ..."
    & sc.exe start $Name | Out-Null
    Start-Sleep -Seconds 2
    if (Test-ServiceRunning $Name) {
        Write-Step "Service $Name is running"
    } else {
        Write-Warn "Service may not have started, check: sc.exe query $Name"
    }
}

# ═══════════════════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════════════════
try {
    Write-Step "============================================"
    Write-Step " BK-CI Agent Session Mode Configuration"
    Write-Step "============================================"
    Write-Step "Work directory: $WorkDir"

    if ($Disable) {
        # ---- Disable session mode ----
        Disable-AutoLogon
        $serviceName = Get-ServiceName
        if (Test-ServiceExists $serviceName) {
            Stop-DaemonSafe  -ServiceName $serviceName
            Start-DaemonService -Name $serviceName
        }
        Write-Step ""
        Write-Step "Session mode disabled. The daemon service is still running"
        Write-Step "but the agent will fall back to Session 0 (no desktop access)"
        Write-Step "unless a user manually logs in."
    } else {
        # ---- Enable session mode (full install) ----

        # Phase 1: prepare
        Initialize-AgentDir

        # Phase 2: service
        $serviceName = Ensure-DaemonService

        # Phase 3: auto-logon
        Enable-AutoLogon -Account $UserName -Pass $Password -SetupLock (-not $NoLock)

        # Phase 4: start
        Start-DaemonService -Name $serviceName

        # Done
        Write-Step ""
        Write-Step "============================================"
        Write-Step " Configuration complete"
        Write-Step "============================================"
        Write-Step "Service       : $serviceName (running)"
        Write-Step "Auto-logon    : $UserName"
        Write-Step "Auto-lock     : $(-not $NoLock)"
        Write-Step ""
        Write-Step "A reboot is required to activate auto-logon."
        Write-Step "After reboot Windows logs in automatically and the"
        Write-Step "daemon launches the agent in the user desktop session."

        if (-not $NoRestart) {
            Write-Step ""
            $answer = Read-Host "Reboot now? (Y/N)"
            if ($answer -match "^[Yy]") {
                Write-Step "Rebooting in 5 seconds..."
                shutdown /r /t 5 /c "BK-CI Agent: activating session mode"
            } else {
                Write-Step "Please reboot manually to activate session mode."
            }
        } else {
            Write-Step "NoRestart specified, please reboot manually."
        }
    }
} catch {
    Write-Host "[BK-CI][ERROR] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

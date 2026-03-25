#Requires -RunAsAdministrator
<#
.SYNOPSIS
    One-step install / configure BK-CI Agent in Session mode (desktop-interactive).

.DESCRIPTION
    Session mode allows the agent (and its build processes) to access the
    logged-in user's desktop for UI testing, screenshots, etc.

    Three levels of session support:

      No credentials (simplest):
        - Installs the daemon as a Windows service.
        - When a user IS logged in the daemon detects the session via WTS API
          and launches the agent there.
        - When NO user is logged in the agent falls back to Session 0.

      With -UserName / -Password:
        - All of the above, PLUS stores credentials in .agent.properties.
        - When NO user is logged in the daemon uses LogonUser with the stored
          credentials to launch the agent in the console session.

      With -UserName / -Password / -AutoLogon:
        - All of the above, PLUS configures Windows auto-logon so that on
          every reboot Windows logs in automatically, guaranteeing a user
          session. Password stored via LSA Secret (encrypted).
          This is a system-wide setting.

    The script is idempotent. Safe to re-run with same or different credentials.

.PARAMETER UserName
    Optional. Windows logon account (e.g. "builduser", "DOMAIN\builduser").
    Required when -AutoLogon is used.

.PARAMETER Password
    Optional. Password for the account. Required when -UserName is specified.
    Validated via LogonUser before saving.

.PARAMETER AutoLogon
    Configure Windows auto-logon on every reboot. Requires -UserName/-Password.

.PARAMETER Disable
    Revert to plain service mode: remove session credentials and auto-logon.

.EXAMPLE
    .\configure_session.ps1
    .\configure_session.ps1 -UserName builduser -Password P@ssw0rd
    .\configure_session.ps1 -UserName builduser -Password P@ssw0rd -AutoLogon
    .\configure_session.ps1 -Disable
#>

param(
    [string]$UserName,
    [string]$Password,
    [switch]$AutoLogon,
    [switch]$Disable
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:WorkDir      = Split-Path -Parent $MyInvocation.MyCommand.Definition
$script:WinlogonPath = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon"
$script:PropsFile    = Join-Path $WorkDir ".agent.properties"

# ═══════════════════════════════════════════════════════════════════════════
# Parameter validation
# ═══════════════════════════════════════════════════════════════════════════
if (-not $Disable) {
    if ($AutoLogon -and [string]::IsNullOrEmpty($UserName)) {
        Write-Host "[BK-CI][ERROR] -AutoLogon requires -UserName and -Password" -ForegroundColor Red
        exit 1
    }
    if (-not [string]::IsNullOrEmpty($UserName) -and [string]::IsNullOrEmpty($Password)) {
        Write-Host "[BK-CI][ERROR] -Password is required when -UserName is specified" -ForegroundColor Red
        exit 1
    }
}

$script:HasCredentials = -not [string]::IsNullOrEmpty($UserName)

# ═══════════════════════════════════════════════════════════════════════════
# Win32 helpers (P/Invoke)
# ═══════════════════════════════════════════════════════════════════════════
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class CredentialValidator
{
    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool LogonUser(
        string lpszUsername, string lpszDomain, string lpszPassword,
        int dwLogonType, int dwLogonProvider, out IntPtr phToken);

    [DllImport("kernel32.dll")]
    private static extern bool CloseHandle(IntPtr hObject);

    private const int LOGON32_LOGON_INTERACTIVE = 2;
    private const int LOGON32_PROVIDER_DEFAULT  = 0;

    public static bool Validate(string user, string domain, string password,
                                out int errorCode)
    {
        IntPtr token;
        bool ok = LogonUser(user, domain, password,
                            LOGON32_LOGON_INTERACTIVE,
                            LOGON32_PROVIDER_DEFAULT,
                            out token);
        errorCode = ok ? 0 : Marshal.GetLastWin32Error();
        if (ok) CloseHandle(token);
        return ok;
    }
}

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

function Get-ServiceName { return "devops_agent_$(Get-AgentId)" }

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
    if ((Test-ServiceExists $ServiceName) -and (Test-ServiceRunning $ServiceName)) {
        Write-Step "Stopping service $ServiceName ..."
        & sc.exe stop $ServiceName 2>$null | Out-Null
        for ($i = 0; $i -lt 20; $i++) {
            Start-Sleep -Seconds 1
            if (-not (Test-ServiceRunning $ServiceName)) { break }
        }
    }
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
    $zipPath  = Join-Path $WorkDir $ZipFile
    $destPath = Join-Path $WorkDir $DestDir
    if (-not (Test-Path $zipPath))  { return }
    if (Test-Path $destPath)        { Write-Step "$DestDir exists, skip unzip"; return }
    Write-Step "Unzipping $ZipFile -> $DestDir"
    if ($PSVersionTable.PSVersion.Major -ge 5) {
        Expand-Archive -Path $zipPath -DestinationPath $destPath -Force
    } else {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [System.IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $destPath)
    }
}

# ═══════════════════════════════════════════════════════════════════════════
# Validate credentials
# ═══════════════════════════════════════════════════════════════════════════
function Assert-Credentials {
    param([string]$Account, [string]$Pass)
    $parsed = Split-UserDomain $Account
    Write-Step "Validating credentials for $($parsed.User)@$($parsed.Domain) ..."
    $errCode = 0
    $valid = [CredentialValidator]::Validate($parsed.User, $parsed.Domain, $Pass, [ref]$errCode)
    if (-not $valid) {
        $errMsg = switch ($errCode) {
            1326    { "wrong username or password" }
            1327    { "account restriction (e.g. logon hours)" }
            1328    { "account logon time restriction" }
            1330    { "password expired" }
            1331    { "account currently disabled" }
            default { "Win32 error $errCode" }
        }
        throw "Credential validation failed: $errMsg"
    }
    Write-Step "Credentials verified OK"
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 1 — Prepare environment
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
    if (-not (Test-Path $daemonExe)) { throw "devopsDaemon.exe not found at $daemonExe" }

    Stop-DaemonSafe -ServiceName $serviceName

    $null = schtasks /query /tn $serviceName 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Step "Removing legacy scheduled task: $serviceName"
        schtasks /delete /tn $serviceName /f 2>$null | Out-Null
    }

    if (Test-ServiceExists $serviceName) {
        Write-Step "Removing existing service to re-create cleanly"
        & sc.exe delete $serviceName 2>$null | Out-Null
        Start-Sleep -Seconds 2
    }

    $binPath = "`"$daemonExe`""
    Write-Step "Creating service: $serviceName"
    & sc.exe create $serviceName binPath= $binPath start= auto | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "sc.exe create failed (exit $LASTEXITCODE)" }
    Write-Step "Service created: $serviceName (auto start)"
    return $serviceName
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 3a — Session credentials in LSA Secret
# ═══════════════════════════════════════════════════════════════════════════
function Save-SessionCredentials {
    param([string]$Account, [string]$Pass)
    [LsaSecret]::Store("BkCiSessionUser", $Account)
    [LsaSecret]::Store("BkCiSessionPassword", $Pass)
    Write-Step "Session credentials stored in LSA Secret (encrypted)"
}

function Remove-SessionCredentials {
    try { [LsaSecret]::Delete("BkCiSessionUser") }     catch {}
    try { [LsaSecret]::Delete("BkCiSessionPassword") } catch {}
    Write-Step "Session credentials removed from LSA Secret"
}

# ═══════════════════════════════════════════════════════════════════════════
# Phase 3b — Windows Auto-Logon (optional)
# ═══════════════════════════════════════════════════════════════════════════
function Enable-AutoLogon {
    param([string]$Account, [string]$Pass)
    $parsed = Split-UserDomain $Account
    Write-Step "Configuring Windows auto-logon: user=$($parsed.User), domain=$($parsed.Domain)"

    Set-ItemProperty -Path $WinlogonPath -Name "AutoAdminLogon"    -Value "1"
    Set-ItemProperty -Path $WinlogonPath -Name "DefaultUserName"   -Value $parsed.User
    Set-ItemProperty -Path $WinlogonPath -Name "DefaultDomainName" -Value $parsed.Domain
    Remove-ItemProperty -Path $WinlogonPath -Name "DefaultPassword" -ErrorAction SilentlyContinue
    Remove-ItemProperty -Path $WinlogonPath -Name "AutoLogonCount"  -ErrorAction SilentlyContinue

    [LsaSecret]::Store("DefaultPassword", $Pass)
    Write-Step "Auto-logon password stored via LSA Secret (encrypted)"

    $powerCfg = "HKLM:\SOFTWARE\Policies\Microsoft\Power\PowerSettings\0e796bdb-100d-47d6-a2d5-f7d2daa51f51"
    if (-not (Test-Path $powerCfg)) { New-Item -Path $powerCfg -Force | Out-Null }
    Set-ItemProperty -Path $powerCfg -Name "DCSettingIndex" -Value 0 -Type DWord
    Set-ItemProperty -Path $powerCfg -Name "ACSettingIndex" -Value 0 -Type DWord
    Write-Step "Auto-logon configured (activates on next reboot)"
}

function Disable-AutoLogon {
    Set-ItemProperty -Path $WinlogonPath -Name "AutoAdminLogon" -Value "0"
    Remove-ItemProperty -Path $WinlogonPath -Name "DefaultPassword" -ErrorAction SilentlyContinue
    try { [LsaSecret]::Delete("DefaultPassword") } catch {}
    Write-Step "Windows auto-logon disabled"
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
    Start-Sleep -Seconds 3
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
        # ---- Disable ----
        Remove-SessionCredentials
        Disable-AutoLogon
        $serviceName = Get-ServiceName
        if (Test-ServiceExists $serviceName) {
            Stop-DaemonSafe     -ServiceName $serviceName
            Start-DaemonService -Name $serviceName
        }
        Write-Step ""
        Write-Step "Done. Session mode disabled."
        Write-Step "The agent will run in Session 0 unless a user is logged in."
    } else {
        # ---- Enable ----
        if ($HasCredentials) {
            Assert-Credentials -Account $UserName -Pass $Password
        }

        Initialize-AgentDir
        $serviceName = Ensure-DaemonService

        if ($HasCredentials) {
            Save-SessionCredentials -Account $UserName -Pass $Password
        }
        if ($AutoLogon) {
            Enable-AutoLogon -Account $UserName -Pass $Password
        }

        Start-DaemonService -Name $serviceName

        # ---- Summary ----
        Write-Step ""
        Write-Step "============================================"
        Write-Step " Done"
        Write-Step "============================================"
        Write-Step "Service      : $serviceName (running)"

        if ($AutoLogon) {
            Write-Step "Session user : $UserName"
            Write-Step "LogonUser    : enabled (fallback when no session)"
            Write-Step "Auto-logon   : enabled (every reboot auto-logs in)"
            Write-Step ""
            Write-Step "The agent is active in your current session NOW."
            Write-Step "On future reboots Windows auto-logs in as $UserName."
            Write-Step "If the password changes, re-run with the new password."
        } elseif ($HasCredentials) {
            Write-Step "Session user : $UserName"
            Write-Step "LogonUser    : enabled (fallback when no session)"
            Write-Step "Auto-logon   : not configured"
            Write-Step ""
            Write-Step "The agent is active in your current session NOW."
            Write-Step "When no user is logged in, daemon uses LogonUser fallback."
            Write-Step "To also auto-logon on reboot, add -AutoLogon."
            Write-Step "If the password changes, re-run with the new password."
        } else {
            Write-Step "Session user : (current logged-in user)"
            Write-Step "LogonUser    : not configured"
            Write-Step "Auto-logon   : not configured"
            Write-Step ""
            Write-Step "The agent is active in your current session NOW."
            Write-Step "When no user is logged in, agent falls back to Session 0."
            Write-Step "To enable fallback, add -UserName and -Password."
        }
    }
} catch {
    Write-Host "[BK-CI][ERROR] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

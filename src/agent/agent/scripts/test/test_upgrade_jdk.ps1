#requires -Version 5.1
param([string]$Jdk8, [string]$Jdk17)
$ErrorActionPreference = 'Stop'
$scriptFile = (Get-Item -LiteralPath (Join-Path $PSScriptRoot '..\upgrade-jdk.ps1')).FullName
$base = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')
$tmp = Join-Path $base ('bkci-jdk-test-' + [guid]::NewGuid().ToString('N'))
$root = Join-Path $tmp 'test agent'
$agent = Join-Path $root 'devopsAgent.exe'
$work = Join-Path $root '.bkci-jdk-upgrade'
function Run-Upgrade($Eight, $Seventeen, [switch]$ShouldFail) {
    & powershell.exe -NoProfile -File $scriptFile -AgentDir $root -Jdk8 $Eight -Jdk17 $Seventeen
    if (($LASTEXITCODE -ne 0) -ne [bool]$ShouldFail) { throw "Unexpected exit $LASTEXITCODE" }
}
try {
    [void][IO.Directory]::CreateDirectory((Join-Path $root 'jre'))
    [IO.File]::WriteAllText((Join-Path $root '.install_type'), 'DIRECT')
    $props = @('devops.agent.id=test', "devops.agent.jdk.dir.path=$root\jre", "devops.agent.jdk17.dir.path=$root\jdk17-123")
    [IO.File]::WriteAllLines((Join-Path $root '.agent.properties'), $props)
    [IO.File]::WriteAllText((Join-Path $root 'jre\old'), '')
    # A real, isolated daemon process makes stop/start observable without registering a machine service.
    Add-Type -OutputAssembly $agent -OutputType ConsoleApplication -TypeDefinition @'
using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
public static class Fixture {
    public static int Main(string[] args) {
        string exe = Process.GetCurrentProcess().MainModule.FileName;
        string dir = Path.GetDirectoryName(exe);
        if (Path.GetFileName(exe) == "java.exe") {
            Console.Error.WriteLine("openjdk version \"" + File.ReadAllText(Path.Combine(dir, "../release")) + "\"");
            return 0;
        }
        if (Path.GetFileName(exe) == "devopsDaemon.exe") { Thread.Sleep(300000); return 0; }
        File.AppendAllText(Path.Combine(dir, "calls"), args[0] + "\n");
        string daemon = Path.Combine(dir, "devopsDaemon.exe");
        if (args[0] == "stop") {
            foreach (Process p in Process.GetProcessesByName("devopsDaemon"))
                if (p.MainModule.FileName == daemon) { p.Kill(); p.WaitForExit(); }
        } else {
            if (File.Exists(Path.Combine(dir, "fail-start"))) return 3;
            Process.Start(new ProcessStartInfo(daemon) { UseShellExecute=false, CreateNoWindow=true });
        }
        return 0;
    }
}
'@
    [IO.File]::Copy($agent, (Join-Path $root 'devopsDaemon.exe'))
    & $agent start
    if ($Jdk8 -and $Jdk17) { Run-Upgrade $Jdk8 $Jdk17; Write-Host 'Real-package restart passed.'; return }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($v in 8,17) {
        $bundle = Join-Path $tmp "package$v"
        [void][IO.Directory]::CreateDirectory((Join-Path $bundle 'bin'))
        [IO.File]::Copy($agent, (Join-Path $bundle 'bin\java.exe'))
        [IO.File]::WriteAllText((Join-Path $bundle 'bin\javac.exe'), '')
        $version = if ($v -eq 8) { '1.8.0_111' } else { '17.0.1' }
        [IO.File]::WriteAllText((Join-Path $bundle 'release'), $version)
        [IO.Compression.ZipFile]::CreateFromDirectory($bundle, (Join-Path $tmp "$v.zip"))
    }
    $eight = Join-Path $tmp '8.zip'
    $seventeen = Join-Path $tmp '17.zip'
    Run-Upgrade $eight $eight -ShouldFail
    if ([IO.File]::ReadAllText((Join-Path $root 'calls')).Trim() -ne 'start') { throw 'Stopped before validation' }
    Run-Upgrade $eight $seventeen
    if (([IO.File]::ReadAllLines((Join-Path $root 'calls')) -join ',') -ne 'start,stop,start' -or
        (Test-Path -LiteralPath $work) -or (Test-Path -LiteralPath (Join-Path $root 'jre\old'))) { throw 'Upgrade failed' }
    [IO.File]::WriteAllText((Join-Path $root 'fail-start'), '')
    Run-Upgrade $eight $seventeen -ShouldFail
    if (!(Test-Path -LiteralPath (Join-Path $work 'old8')) -or !(Test-Path -LiteralPath (Join-Path $work 'old17'))) {
        throw 'Startup failure lost backups'
    }
    Write-Host 'Windows stop/replace/start tests passed.'
} finally {
    if ([IO.File]::Exists($agent)) { & $agent stop }
    if ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($tmp)) -eq $base -and
        [IO.Path]::GetFileName($tmp) -match '^bkci-jdk-test-[a-f0-9]{32}\z' -and [IO.Directory]::Exists($tmp)) {
        [IO.Directory]::Delete($tmp, $true)
    }
}

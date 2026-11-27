#requires -Version 5.1
param(
    [Parameter(Mandatory, Position=0)][string]$AgentDir,
    [Parameter(Mandatory, Position=1)][string]$Jdk8,
    [Parameter(Mandatory, Position=2)][string]$Jdk17
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = (Get-Item -LiteralPath $AgentDir).FullName.TrimEnd('\')
$work = Join-Path $root '.bkci-jdk-upgrade'
$agent = Join-Path $root 'devopsAgent.exe'
$settings = Join-Path $root '.agent.properties'

function Read-Settings {
    $props = @{}
    foreach ($line in [IO.File]::ReadAllLines($settings)) {
        if ($line -match '^\s*\[') { break }
        if ($line -match '^\s*(devops\.agent\.(?:id|jdk(?:17)?\.dir\.path))\s*[=:]\s*(.*)$') {
            $props[$Matches[1]] = $Matches[2].Trim()
        }
    }
    return $props
}

function Get-Target($Props, [int]$Version) {
    $key = if ($Version -eq 8) { 'jdk' } else { 'jdk17' }
    $path = $Props["devops.agent.$key.dir.path"]
    if (!$path) { $path = Join-Path $root $key }
    $path = [IO.Path]::GetFullPath($path)
    $name = [IO.Path]::GetFileName($path)
    $pattern = if ($Version -eq 8) { '^(jdk|jre)(-[0-9]+)?\z' } else { '^jdk17(-[0-9]+)?\z' }
    $item = Get-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    if ([IO.Path]::GetDirectoryName($path) -ne $root -or $name -notmatch $pattern -or
        ($item -and ($item.Attributes -band [IO.FileAttributes]::ReparsePoint))) {
        throw "Expected an Agent-private JDK $Version directory: $path"
    }
    return $path
}

function Prepare-Jdk([int]$Version, [string]$Source) {
    if ($Source -match '^https?://') {
        $archive = Join-Path $work "$Version.zip"
        [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri $Source -OutFile $archive
    } else { $archive = (Get-Item -LiteralPath $Source).FullName }
    $unpack = Join-Path $work "unpack$Version"
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, $unpack)
    $homes = @(Get-ChildItem -LiteralPath $unpack -File -Recurse -Filter javac.exe | Where-Object {
        $_.Directory.Name -eq 'bin' -and [IO.File]::Exists((Join-Path $_.Directory.Parent.FullName 'release'))
    })
    if ($homes.Count -ne 1) { throw "Expected one complete JDK $Version in the package" }
    $dest = Join-Path $work "new$Version"
    Move-Private $homes[0].Directory.Parent.FullName $dest
    # java -version writes to stderr, including on successful Windows PowerShell 5.1 runs.
    $java = Join-Path $dest 'bin\java.exe'
    $output = (& { $ErrorActionPreference = 'Continue'; & $java -version 2>&1 }) -join [Environment]::NewLine
    $major = if ($Version -eq 8) { '1\.8' } else { '17' }
    if ($LASTEXITCODE -ne 0 -or $output -notmatch "(?m)(?:openjdk|java) version \x22$major[._+\x22-]") {
        throw "Expected a working Java $Version. $output"
    }
    Write-Host $output
}

function Get-InstanceProcess([string]$Name) {
    Get-Process -Name $Name -ErrorAction SilentlyContinue | Where-Object {
        $_.Path -eq (Join-Path $root "$Name.exe")
    }
}

function Control-Agent([string]$Action) {
    if ($controller -eq 'cli') {
        & $agent $Action
        if ($LASTEXITCODE -ne 0) { throw "Agent $Action failed ($LASTEXITCODE)" }
    } elseif ($controller -eq 'service') {
        if ($Action -eq 'stop') { Stop-Service -Name $serviceName } else { Start-Service -Name $serviceName }
    } elseif ($Action -eq 'start') { Start-ScheduledTask -TaskName $serviceName -TaskPath '\' }
    else {
        Stop-ScheduledTask -TaskName $serviceName -TaskPath '\'
        # The legacy task launches detached children via devopsctl.vbs.
        foreach ($name in 'devopsDaemon','devopsAgent') { Get-InstanceProcess $name | Stop-Process -Force }
    }
    for ($i = 0; $i -lt 30; $i++) {
        $daemon = Get-InstanceProcess 'devopsDaemon'
        if ($Action -eq 'start' -and $daemon) { return }
        if ($Action -eq 'stop' -and !$daemon -and !(Get-InstanceProcess 'devopsAgent')) { return }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for Agent $Action in $root"
}

# All renames stay within this installation. Directory.Move also rejects an existing destination.
function Move-Private([string]$Source, [string]$Dest) {
    foreach ($path in $Source,$Dest) {
        if (![IO.Path]::GetFullPath($path).StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw "Path outside Agent directory: $path"
        }
    }
    for ($attempt = 0; ; $attempt++) {
        try {
            if ([IO.File]::Exists($Source)) { [IO.File]::Move($Source, $Dest) }
            else { [IO.Directory]::Move($Source, $Dest) }
            return
        } catch {
            if ($attempt -ge 10 -or ($_.Exception.GetBaseException().HResult -band 65535) -notin 5,32,33) { throw }
            Start-Sleep -Milliseconds 250
        }
    }
}

if (![IO.File]::Exists($agent)) { throw "Expected an Agent installation at $root" }
$props = Read-Settings
$id = $props['devops.agent.id']
if ($id -notmatch '^[a-zA-Z0-9_-]+\z') { throw 'Invalid devops.agent.id' }
$serviceName = "devops_agent_$id"
if (Test-Path -LiteralPath (Join-Path $root '.install_type')) { $controller = 'cli' }
elseif (Get-Service -Name $serviceName -ErrorAction SilentlyContinue) { $controller = 'service' }
elseif (Get-ScheduledTask -TaskName $serviceName -TaskPath '\' -ErrorAction SilentlyContinue) { $controller = 'task' }
else { throw "Cannot find the installed Agent service or scheduled task: $serviceName" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
# Atomic directory creation also excludes concurrent upgrades.
[void](New-Item -Path $work -ItemType Directory)
$phase = 'prepare'
try {
    Prepare-Jdk 8 $Jdk8
    Prepare-Jdk 17 $Jdk17
    $props = Read-Settings
    $targets = @{}
    foreach ($v in 8,17) {
        $targets[$v] = Get-Target $props $v
        Write-Host "JDK $v -> $($targets[$v])"
    }
    [IO.File]::WriteAllLines((Join-Path $work 'targets'), [string[]]@($targets[8], $targets[17]))
    Control-Agent stop
    $phase = 'replace'
    $props = Read-Settings
    foreach ($v in 8,17) {
        $dest = $targets[$v]
        if ((Get-Target $props $v) -ne $dest) { throw 'JDK configuration changed during preparation' }
        if (Test-Path -LiteralPath $dest) { Move-Private $dest (Join-Path $work "old$v") }
        Move-Private (Join-Path $work "new$v") $dest
    }
    $phase = 'starting'
    Control-Agent start
    $phase = 'done'
    Write-Host 'Agent restarted with the new JDKs.'
} finally {
    if ($phase -eq 'replace') {
        Write-Warning "Restoring previous JDKs. If recovery fails, keep $work"
        foreach ($v in 17,8) {
            $new = Join-Path $work "new$v"
            $old = Join-Path $work "old$v"
            if (!(Test-Path -LiteralPath $new)) { Move-Private $targets[$v] $new }
            if (Test-Path -LiteralPath $old) { Move-Private $old $targets[$v] }
        }
        Control-Agent start
    }
    if ($phase -eq 'starting') { Write-Warning "Check Agent startup. Previous JDKs are in $work\old8 and old17." }
    elseif ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($work)) -eq $root -and
            !((Get-Item -LiteralPath $work -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        [IO.Directory]::Delete($work, $true)
    }
}

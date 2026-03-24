function Unzip-File {
    param(
        [string]$ZipFile,
        [string]$Destination
    )
    $psMajor = $PSVersionTable.PSVersion.Major

    if ($psMajor -ge 5) {
        Expand-Archive -Path $ZipFile -DestinationPath $Destination -Force
    } else {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipFile, $Destination)
    }
}

function Check-Files {
    if ("##enableCheckFiles##" -eq "true") {
        $fileCount = (Get-ChildItem -File -Path . | Measure-Object).Count
        if ($fileCount -gt 0) {
            Write-Host "fatal: current directory is not empty, please install in an empty directory" -ForegroundColor Red
            Pause
            return
        }
    }
}

$ProgressPreference = 'SilentlyContinue'

Check-Files

$Uri = '##agent_url##'
$InvalidHeaders = @{
    'X-DEVOPS-PROJECT-ID' = '##projectId##'
}
Write-Host "start download agent.zip"
Invoke-WebRequest -Uri $Uri -Headers $InvalidHeaders -OutFile agent.zip
if (-not $?) {
    Write-Host "Invoke-WebRequest agent.zip error" -ForegroundColor Red
    Pause
    return
}

Write-Host "agent.zip downloaded. start unzip it"
Unzip-File "$PWD/agent.zip" "$PWD"
if (-not $?) {
    Write-Host "unzip agent.zip error" -ForegroundColor Red
    Pause
    return
}
Write-Host "unzip agent.zip succ" -ForegroundColor Green

Write-Host "start install devops agent"
$agent_id = "##agentId##"
Write-Host "agent_id=$agent_id"
$service_name = "devops_agent_$agent_id"
Write-Host "service_name=$service_name"

if ($MyInvocation.MyCommand.Path) {
    $work_dir = Split-Path -Parent $MyInvocation.MyCommand.Path
} else {
    $work_dir = Get-Location
}
Write-Host "work_dir: $work_dir"
Set-Location $work_dir

if (Test-Path "jdk17.zip") {
    if (Test-Path "jdk17") {
        Write-Host "jdk17 already exists, skip unzip"
    } else {
        Write-Host "unzip jdk17"
        Unzip-File "$work_dir\jdk17.zip" "$work_dir\jdk17"
    }
} else {
    Write-Host "'jdk17.zip' is not exist" -ForegroundColor Yellow
}

if (Test-Path "jre.zip") {
    if (Test-Path "jdk") {
        Write-Host "jdk already exists, skip unzip"
    } else {
        Write-Host "unzip jdk"
        Unzip-File "$work_dir\jre.zip" "$work_dir\jdk"
    }
} else {
    Write-Host "'jre.zip' is not exist" -ForegroundColor Yellow
}

New-Item -ItemType Directory -Force -Path "$work_dir\logs" | Out-Null
New-Item -ItemType Directory -Force -Path "$work_dir\workspace" | Out-Null

Set-Content -Path "$work_dir\.install_type" -Value "SERVICE" -NoNewline

$service = Get-Service -Name $service_name -ErrorAction SilentlyContinue
if (-not $service) {
    sc.exe create $service_name binPath= "$work_dir\devopsDaemon.exe" start= auto
    Write-Host "install agent service" -ForegroundColor Green
}
sc.exe start $service_name
Write-Host "start agent service" -ForegroundColor Green

Remove-Item -Path "$work_dir\download_install.ps1" -Force

Pause
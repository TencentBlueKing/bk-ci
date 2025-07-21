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
            exit 1
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
    exit 1
}

Write-Host "agent.zip downloaded. start unzip it"
Unzip-File "$PWD/agent.zip" "$PWD"
if (-not $?) {
    Write-Host "unzip agent.zip error" -ForegroundColor Red
    exit 1
}
Write-Host "unzip agent.zip succ" -ForegroundColor Green

Write-Host "start install devops agent"
$agent_id = "##agentId##"
Write-Host "agent_id=$agent_id"
$service_name = "devops_agent_$agent_id"
Write-Host "service_name=$service_name"
$service_username = "##serviceUsername##"
Write-Host "service_username=$service_username"
$service_password = "##servicePassword##"
Write-Host "service_password=$service_password"
$install_type = "##installType##"
Write-Host "install_type=$install_type"

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

if ([string]::IsNullOrEmpty($install_type) -or $install_type -eq "SERVICE") {
    $service = Get-Service -Name $service_name -ErrorAction SilentlyContinue
    if (-not $service) {
        sc.exe create $service_name binPath= "$work_dir\devopsDaemon.exe" start= auto
        Write-Host "install agent service" -ForegroundColor Green
    }
    sc.exe start $service_name
    Write-Host "start agent service" -ForegroundColor Green
    if (![string]::IsNullOrEmpty($service_username) -and (![string]::IsNullOrEmpty($service_password))) {
        Write-Host "both service_username and service_password are defined"
        sc.exe config $service_name obj= $service_username password= $service_password
        if ($LASTEXITCODE -eq 0) {
            Write-Host "service login credentials updated successfully" -ForegroundColor Green
        } else {
            Write-Host "failed to update service login credentials" -ForegroundColor Red
        }
    }
} elseif ($install_type -eq "TASK") {
    Write-Host "Creating scheduled task to run $service_name when any user logs on..."
    schtasks /create /tn $service_name /tr "cscript  $work_dir\devopsctl.vbs" /sc ONLOGON /F
    cscript devopsctl.vbs
    Write-Host "start agent task" -ForegroundColor Green
} else {
    Write-Host "Unknown install_type: $install_type" -ForegroundColor Red
}

Remove-Item -Path "$work_dir\download_install.ps1" -Force

Pause
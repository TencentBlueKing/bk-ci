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



# 不显示进度条用来提速
$ProgressPreference = 'SilentlyContinue'

# 检查当前目录是否有文件
Check-Files

# 下载agent.zip
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

# 解压缩agent.zip
Write-Host "agent.zip downloaded. start unzip it"
Unzip-File "$PWD/agent.zip" "$PWD"
if (-not $?) {
    Write-Host "unzip agent.zip error" -ForegroundColor Red
    exit 1
}
Write-Host "unzip agent.zip succ" -ForegroundColor Green

# 安装 agent
$agent_id = "##agentId##"
$service_name = "devops_agent_$agent_id"
$service_username = "##serviceUsername##"
$service_password = "##servicePassword##"

Write-Host "start install devops agent $service_name $service_username $service_password"
if ($MyInvocation.MyCommand.Path) {
    $work_dir = Split-Path -Parent $MyInvocation.MyCommand.Path
} else {
    $work_dir = Get-Location
}
Write-Host "work_dir: $work_dir"
Set-Location $work_dir

# 解压 jdk17.zip
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

# 解压 jre.zip
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

# 创建 logs 和 workspace 目录
New-Item -ItemType Directory -Force -Path "$work_dir\logs" | Out-Null
New-Item -ItemType Directory -Force -Path "$work_dir\workspace" | Out-Null

# 检查计划任务
$taskExists = schtasks /query | Select-String $service_name

if (-not $taskExists) {
    # 检查并安装服务服务
    $service = Get-Service -Name $service_name -ErrorAction SilentlyContinue
    if (-not $service) {
        Write-Host "install agent service" -ForegroundColor Green
        sc.exe create $service_name binPath= "$work_dir\devopsDaemon.exe" start= auto
    }
    Write-Host "start agent service" -ForegroundColor Green
    sc.exe start $service_name
    # 登录服务
    if (![string]::IsNullOrEmpty($service_username) -and (![string]::IsNullOrEmpty($service_password))) {
        Write-Host "both service_username and service_password are defined"
        sc.exe config $service_name "obj= $service_username" "password= $service_password"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "service login credentials updated successfully" -ForegroundColor Green
        } else {
            Write-Host "failed to update service login credentials" -ForegroundColor Red
        }
    }
} else {
    Write-Host "start devops daemon by devopsctl.vbs"
    Start-Process "wscript.exe" -ArgumentList "$work_dir\devopsctl.vbs"
}

# 删除下载安装脚本
Remove-Item -Path "$work_dir\download_install.ps1" -Force

Pause
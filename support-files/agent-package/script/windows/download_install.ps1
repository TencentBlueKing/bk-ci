function Unzip-File {
    param(
        [string]$ZipFile,
        [string]$Destination
    )
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($ZipFile)
    try {
        foreach ($entry in $archive.Entries) {
            $destPath = Join-Path $Destination $entry.FullName
            if ($entry.FullName.EndsWith('/') -or $entry.FullName.EndsWith('\')) {
                New-Item -ItemType Directory -Force -Path $destPath | Out-Null
                continue
            }
            $destDir = Split-Path -Parent $destPath
            if (-not (Test-Path $destDir)) {
                New-Item -ItemType Directory -Force -Path $destDir | Out-Null
            }
            try {
                $stream = $entry.Open()
                $file = [System.IO.File]::Create($destPath)
                $stream.CopyTo($file)
                $file.Close()
                $stream.Close()
            } catch {
                Write-Host "[WARN] skip locked file: $($entry.FullName) ($($_.Exception.Message))" -ForegroundColor Yellow
            }
        }
    } finally {
        $archive.Dispose()
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
try {
    $response = Invoke-WebRequest -Uri $Uri -Headers $InvalidHeaders -OutFile agent.zip -PassThru -UseBasicParsing
    Write-Host "download complete (HTTP $($response.StatusCode))" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "download agent.zip failed (HTTP $statusCode)" -ForegroundColor Red
    try {
        $errStream = $_.Exception.Response.GetResponseStream()
        if ($errStream) {
            $reader = New-Object System.IO.StreamReader($errStream)
            $errBody = $reader.ReadToEnd()
            $reader.Close()
            Write-Host "server response:" -ForegroundColor Yellow
            Write-Host $errBody
        }
    } catch {
        Write-Host "error detail: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    Remove-Item -Path agent.zip -Force -ErrorAction SilentlyContinue
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

$agent_exe = "$work_dir\devopsAgent.exe"
Write-Host "Installing agent service via CLI..."
& $agent_exe install
if ($LASTEXITCODE -eq 0) {
    Write-Host "agent service installed and started" -ForegroundColor Green
} else {
    Write-Host "agent install failed (exit $LASTEXITCODE)" -ForegroundColor Red
}

if (![string]::IsNullOrEmpty($service_username) -and (![string]::IsNullOrEmpty($service_password))) {
    Write-Host "configuring service login credentials..."
    sc.exe config $service_name obj= $service_username password= $service_password
    if ($LASTEXITCODE -eq 0) {
        Write-Host "service login credentials updated" -ForegroundColor Green
    } else {
        Write-Host "failed to update service login credentials" -ForegroundColor Red
    }
}

Pause
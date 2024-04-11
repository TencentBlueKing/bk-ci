# 不显示进度条用来提速
$ProgressPreference = 'SilentlyContinue'

# 下载agent.zip
$Uri = '##agent_url##'
$InvalidHeaders = @{
    'X-DEVOPS-PROJECT-ID' = '##projectId##'
}

Write-Host "start download agent.zip"

Invoke-WebRequest -Uri $Uri -Headers $InvalidHeaders -OutFile agent.zip
if (-not $?) {
    Write-Host "Invoke-WebRequest agent.zip error"
    exit 1
}

$ProgressPreference = 'Continue'

Write-Host "agent.zip downloaded. start unzip it"

# 解压缩agent.zip
Add-Type -AssemblyName System.IO.Compression.FileSystem ; [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/agent.zip", "$PWD")
if (-not $?) {
    Write-Host "unzip agent.zip error"
    exit 1
}

Write-Host "unzip agent.zip succ. start install it"

# 安装agent
Start-Process -FilePath "$PWD/install.bat"
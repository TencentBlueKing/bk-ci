# 下载agent.zip
$Uri = '##agent_url##'
$InvalidHeaders = @{
    'X-DEVOPS-PROJECT-ID' = '##projectId##'
}
Invoke-WebRequest -Uri $Uri -Headers $InvalidHeaders -OutFile agent.zip
if (-not $?) {
    Write-Host "Invoke-WebRequest agent.zip error"
    exit 1
}

# 解压缩agent.zip
Add-Type -AssemblyName System.IO.Compression.FileSystem ; [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/agent.zip", "$PWD")
if (-not $?) {
    Write-Host "unzip agent.zip error"
    exit 1
}

# 安装agent
Start-Process -FilePath "$PWD/install.bat"
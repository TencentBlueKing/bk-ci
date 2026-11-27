#requires -Version 5.1
param(
    [Parameter(Position=0)][string]$Action,
    [Parameter(Position=1)][string]$AgentDir,
    [Parameter(Position=2)][string]$Package
)
if ($Action -notin 'upgrade','rollback','confirm' -and $AgentDir -and !$Package) {
    $Package = $AgentDir
    $AgentDir = $Action
    $Action = 'upgrade'
}
if (!$AgentDir -or $Action -notin 'upgrade','rollback','confirm' -or ($Action -eq 'upgrade' -and !$Package)) {
    [Console]::Error.WriteLine('用法: upgrade.ps1 安装目录 包URL或本地ZIP | rollback 安装目录 | confirm 安装目录')
    exit 2
}
$ErrorActionPreference = 'Stop'
$step = '检查安装目录'
$itemName = ''
$root = [IO.Path]::GetFullPath($ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($AgentDir))
$state = Join-Path $root '.bkci-script-upgrade'
$pending = Join-Path $state 'pending'
$stage = Join-Path $pending 'new'
$backup = Join-Path $pending 'old'
$journal = Join-Path $pending 'journal.json'
$owned = Join-Path $state 'owned.txt'
$applied = Join-Path $pending 'applied'
$guard = $null
$log = $null
$preparedHere = $false
$restartNeeded = $false
$complete = Join-Path $pending 'complete'
$restart = Join-Path $pending 'restart'

function Report([string]$message) {
    Write-Host $message
    if ($log) {
        try { [IO.File]::AppendAllText($log, $message + [Environment]::NewLine) }
        catch { Write-Warning "无法写入日志 $log，$($_.Exception.Message)" }
    }
}

function Check-Path([string]$path) {
    $info = Get-Item -LiteralPath $path -Force -ErrorAction Ignore
    if ($info -and ($info.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw "路径是链接或联接点：$path"
    }
}

function Check-Name([string]$name) {
    $parts = $name.Split('/')
    foreach ($part in $parts) {
        if ($part -notmatch '^[a-z0-9_+-][a-z0-9_.+-]*\z' -or $part.EndsWith('.') -or
            $part -match '^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(\.|$)') { throw "不支持的包路径：$name" }
    }
    if ($parts[0] -in 'workspace','runtime','logs','tmp','upgrade','config','conf','cert','data','bkci-package-target' -or
        $name -match '\.(pid|lock|properties)$') { throw "包中含有受保护文件：$name" }
}

function Check-Destination([string]$name) {
    Check-Name $name
    $path = $root
    foreach ($part in $name.Split('/')) {
        if ([IO.File]::Exists($path)) { throw "父路径不是目录：$path" }
        $path = Join-Path $path $part
        Check-Path $path
    }
    if ([IO.Directory]::Exists($path)) { throw "目标不是普通文件：$path" }
}

function Instance-Processes([string]$name) {
    Get-Process -Name $name -ErrorAction SilentlyContinue | Where-Object {
        $_.Path -and $_.Path -eq (Join-Path $root ($name + '.exe'))
    }
}

# This snapshot is for reporting and the startup guard only.
function Get-InstanceReport {
    foreach ($name in 'devopsDaemon','devopsAgent','devopsCollector') {
        $expected = Join-Path $root ($name + '.exe')
        $previous = Join-Path $backup ($name + '.exe')
        foreach ($proc in @(Get-Process -Name $name -ErrorAction SilentlyContinue)) {
            $path = $proc.Path
            if ($path -eq $expected -or $path -eq $previous) { "$path PID=$($proc.Id)" }
            elseif (!$path -and !$proc.HasExited) { "$name PID=$($proc.Id)（路径待确认）" }
        }
    }
}

function Select-Controller {
    $line = Select-String -LiteralPath (Join-Path $root '.agent.properties') -Pattern '^\s*devops\.agent\.id\s*=\s*([a-zA-Z0-9_-]+)\s*$' | Select-Object -First 1
    if (!$line) { throw '无法读取 devops.agent.id' }
    $script:serviceName = 'devops_agent_' + $line.Matches[0].Groups[1].Value
    $daemon = Join-Path $root 'devopsDaemon.exe'
    $modeFile = Join-Path $root '.install_type'
    $mode = if ([IO.File]::Exists($modeFile)) { [IO.File]::ReadAllText($modeFile).Trim() }
    $svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if ($svc -and $mode -ne 'TASK') {
        $image = (Get-ItemProperty -LiteralPath "HKLM:\SYSTEM\CurrentControlSet\Services\$serviceName").ImagePath
        if ([IO.Path]::GetFullPath([Environment]::ExpandEnvironmentVariables($image).Trim('"')) -ne $daemon) {
            throw "服务 $serviceName 的程序路径与 $root 不符"
        }
        $script:controller = 'service'
    } else {
        $scheduler = New-Object -ComObject Schedule.Service
        $scheduler.Connect()
        try { $script:task = $scheduler.GetFolder('\').GetTask($serviceName) }
        catch [System.IO.FileNotFoundException] { $script:task = $null }
        catch [System.Runtime.InteropServices.COMException] {
            if ($_.Exception.HResult -ne -2147024894) { throw }
            $script:task = $null
        }
        if ($task) {
            $actions = $task.Definition.Actions
            $act = $actions.Item(1)
            $exe = [Environment]::ExpandEnvironmentVariables($act.Path).Trim('"')
            $vbs = Join-Path $root 'devopsctl.vbs'
            $wrapper = [IO.Path]::GetFileNameWithoutExtension($exe) -in 'cscript','wscript'
            if ($actions.Count -ne 1 -or !($exe -eq $daemon -or
                ($wrapper -and [IO.Path]::GetFullPath($act.Arguments.Trim().Trim('"')) -eq $vbs))) {
                throw "计划任务 $serviceName 的启动路径与 $root 不符"
            }
            $script:controller = 'task'
        } elseif ($mode -in 'TASK','SERVICE','SESSION') { throw "找不到 $mode 注册的 $serviceName" }
        else { $script:controller = 'direct' }
    }
    Report "启停方式 $controller；实例 $serviceName"
}

function Check-Upgrader {
    if (Instance-Processes 'upgrader') { Report '提醒：本实例的自动升级进程仍在运行，请核对最终版本' }
}

function Stop-Instance {
    $report = @(Get-InstanceReport)
    if ($report) { Report "停机前进程检查：$($report -join ', ')" }
    else { Report "停机前进程检查：未发现 $root 的进程" }
    Check-Upgrader
    try {
        if ($controller -eq 'service') {
            $svc = Get-Service -Name $serviceName
            if ($svc.Status -notin 'Stopped','StopPending') { $svc.Stop() }
            $svc.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
        } elseif ($controller -eq 'task' -and $task.State -in 2,4) { $task.Stop(0) }
    } catch { Report "提醒：服务停止失败，继续按可执行文件路径检查。$($_.Exception.Message)" }
    # Legacy tasks can leave their daemon running after wscript has exited.
    foreach ($name in 'devopsDaemon','devopsAgent','devopsCollector') {
        foreach ($proc in @(Instance-Processes $name)) {
            try {
                if ($proc.HasExited -or $proc.Path -ne (Join-Path $root ($name + '.exe'))) { continue }
                Report "停止已确认的 $($proc.Path) (PID $($proc.Id))"
                $proc.Kill()
                if (!$proc.WaitForExit(10000)) { Report "提醒：等待 $name (PID $($proc.Id)) 停止超时" }
            } catch { Report "提醒：停止 $name (PID $($proc.Id)) 时发生错误。$($_.Exception.Message)" }
        }
    }
    foreach ($name in 'devopsDaemon','devopsAgent','devopsCollector') {
        if (Instance-Processes $name) { throw "$name 又被启动，请检查本实例的服务管理器" }
    }
    Check-Upgrader
}

function Start-Instance {
    $report = @(Get-InstanceReport)
    if ($report) {
        Report "提醒：跳过启动，避免重复实例；请人工核对：$($report -join ', ')"
        return
    }
    if ($controller -eq 'service') {
        $svc = Get-Service -Name $serviceName
        if ($svc.Status -notin 'Running','StartPending') { $svc.Start() }
    } elseif ($controller -eq 'task') {
        if (!(Instance-Processes 'devopsDaemon')) { [void]$task.Run($null) }
    } elseif (!(Instance-Processes 'devopsDaemon')) {
        Start-Process -FilePath (Join-Path $root 'devopsDaemon.exe') -WorkingDirectory $root -WindowStyle Hidden
    }
    $previous = ''
    for ($i = 0; $i -lt 30; $i++) {
        $daemon = Instance-Processes 'devopsDaemon'
        $agent = Instance-Processes 'devopsAgent'
        $pair = "$($daemon.Id):$($agent.Id)"
        if ($daemon -and $agent -and $pair -eq $previous) {
            if (@($daemon).Count -gt 1 -or @($agent).Count -gt 1) {
                Report "提醒：$root 有多个稳定进程，保留全部供核对（Daemon:Agent PID）：$pair"
            }
            return
        }
        $previous = $pair
        Start-Sleep -Seconds 1
    }
    throw "等待本目录的 Daemon 和 Agent 启动超时，请查看 $root\logs"
}

function Finish-Transaction {
    $done = Join-Path $state ('done-' + [guid]::NewGuid())
    [IO.Directory]::Move($pending, $done)
    try { [IO.Directory]::Delete($done, $true) }
    catch { Report "已结束，临时文件待清理：$done，$($_.Exception.Message)" }
}

function Restore-Files {
    [IO.File]::Delete($complete)
    [IO.File]::Delete($applied)
    $plan = Get-Content -LiteralPath $journal -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($entry in $plan.files) {
        $name = $entry.name
        Check-Destination $name
        $dest = Join-Path $root $name
        $old = Join-Path $backup $name
        if ($entry.existed -and [IO.File]::Exists($old)) {
            [IO.File]::Delete($dest)
            [IO.File]::Move($old, $dest)
        } elseif (!$entry.existed -and ![IO.File]::Exists((Join-Path $stage $name))) {
            [IO.File]::Delete($dest)
        }
    }
    foreach ($dir in $plan.dirs) {
        $path = Join-Path $root $dir
        if ([IO.Directory]::Exists($path) -and [IO.Directory]::GetFileSystemEntries($path).Length -eq 0) {
            [IO.Directory]::Delete($path)
        }
    }
    [IO.File]::Copy((Join-Path $pending 'owned.before'), $owned, $true)
}

function Invoke-ProcessStep([string]$operation) {
    try {
        try { Select-Controller } catch {
            if ($operation -ne 'stop') { throw }
            Report "提醒：无法确认服务注册，按可执行文件路径检查。$($_.Exception.Message)"
            $script:controller = 'none'
        }
        if ($operation -eq 'stop') { Stop-Instance } else { Start-Instance }
    } catch { Report "提醒：$operation 进程操作未完成，$($_.Exception.Message)。请人工核对服务状态。" }
}

function Recover-Instance {
    if ([IO.File]::Exists($journal)) { Invoke-ProcessStep stop; Restore-Files }
    Invoke-ProcessStep start
    Finish-Transaction
    Report '原版本文件已恢复。'
}

function Save-Owned {
    $plan = Get-Content -LiteralPath $journal -Raw -Encoding UTF8 | ConvertFrom-Json
    $all = @([IO.File]::ReadAllLines((Join-Path $pending 'owned.before'))) + @($plan.files.name)
    [IO.File]::WriteAllLines($owned, [string[]]@($all | Sort-Object -Unique))
}

function Read-Package([string]$path) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        $marker = $zip.GetEntry('bkci-package-target')
        if (!$marker -or $marker.Length -gt 64) { throw '缺少有效的平台标记；请用配套的 pack.py 打包' }
        $reader = [IO.StreamReader]::new($marker.Open())
        try { $target = $reader.ReadToEnd().Trim() } finally { $reader.Dispose() }
        if ($target -notin 'windows-amd64','windows-386' -or
            ($target -eq 'windows-amd64' -and ![Environment]::Is64BitOperatingSystem)) {
            throw "平台不匹配：$target"
        }
        $seen = @{}
        foreach ($entry in $zip.Entries) {
            $name = $entry.FullName
            if ($seen.ContainsKey($name)) { throw "包中有重复路径：$name" }
            $seen[$name] = $true
            if ($name -ceq 'bkci-package-target') { continue }
            Check-Name $name
            $dest = Join-Path $stage $name
            [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($dest))
            try { [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest) }
            catch { throw "解压 $name 失败：$($_.Exception.Message)" }
            $name
        }
    } finally { $zip.Dispose() }
}

function New-Plan($names, $known) {
    $dirs = @{}
    $files = foreach ($name in $names) {
        Check-Destination $name
        $exists = [IO.File]::Exists((Join-Path $root $name))
        if ($exists -and !$known.ContainsKey($name) -and
            $name -notmatch '^(devopsAgent\.exe|devopsDaemon\.exe|devopsCollector\.exe|upgrader\.exe|worker-agent\.jar|agent_docker_init\.sh|jdk17/.+|jdk/.+)$') {
            throw "同名文件已存在且不属于发布程序：$name"
        }
        $dir = [IO.Path]::GetDirectoryName($name)
        while ($dir -and ![IO.Directory]::Exists((Join-Path $root $dir))) {
            $dirs[$dir] = $true
            $dir = [IO.Path]::GetDirectoryName($dir)
        }
        [pscustomobject]@{name=$name; existed=$exists}
    }
    [pscustomobject]@{files=@($files); dirs=@($dirs.Keys | Sort-Object -Descending)}
}

try {
    Check-Path $root
    Check-Path (Join-Path $root '.agent.properties')
    if (![IO.File]::Exists((Join-Path $root '.agent.properties'))) { throw "缺少 $root\.agent.properties" }
    Check-Path $state
    [void][IO.Directory]::CreateDirectory($state)
    $log = Join-Path $state ("run-" + [guid]::NewGuid() + ".log")
    Report "安装目录 $root；日志 $log"
    $step = '获取手动升级锁'
    $guard = [IO.File]::Open((Join-Path $state 'running.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    $step = '检查上次升级记录'
    Check-Path $pending
    if ($Action -eq 'upgrade') {
        if (!$Package) { throw 'upgrade 需要内网包 URL 或本地 ZIP 路径' }
        if ([IO.File]::Exists($complete)) { Finish-Transaction }
        if ([IO.Directory]::Exists($pending)) {
            if ([IO.File]::Exists($applied)) {
                throw "上次已替换文件。已核实该版本和心跳正常可执行 confirm；需要恢复原版本请执行 rollback。记录：$pending"
            }
            throw "上次升级未完成，请先执行 rollback，再重试升级。记录：$pending"
        }
        [void][IO.Directory]::CreateDirectory($stage)
        $preparedHere = $true
        $step = '1/4 获取升级包'
        Report "[$step]"
        $pkg = Join-Path $pending 'package.zip'
        if ($Package -match '^https?://') { Invoke-WebRequest -UseBasicParsing -ErrorAction Stop -Uri $Package -OutFile $pkg }
        else { [IO.File]::Copy($ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Package), $pkg) }
        $step = '2/4 检查包和目标文件'
        Report "[$step]"
        $names = @(Read-Package $pkg)
        if ($names.Count -eq 0) { throw '包中没有升级文件' }
        [IO.File]::Delete($pkg)
        if (![IO.File]::Exists($owned)) { [IO.File]::WriteAllText($owned, '') }
        [IO.File]::Copy($owned, (Join-Path $pending 'owned.before'))
        $known = @{}
        foreach ($name in [IO.File]::ReadAllLines($owned)) { $known[$name] = $true }
        $plan = New-Plan $names $known
        foreach ($file in $plan.files) {
            $verb = if ($file.existed) { '替换' } else { '新增' }
            Report "  $verb $($file.name)"
        }
        $step = '3/4 停机替换'
        Report "[$step] 尝试停止 $root 的进程"
        [IO.File]::WriteAllText($restart, '')
        $restartNeeded = $true
        Invoke-ProcessStep stop
        $plan = New-Plan $names $known
        $draft = Join-Path $pending 'journal.tmp'
        [IO.File]::WriteAllText($draft, ($plan | ConvertTo-Json -Depth 4))
        [IO.File]::Move($draft, $journal)
        foreach ($file in $plan.files) {
            $itemName = $file.name
            Report "[$step] $itemName"
            $dest = Join-Path $root $itemName
            [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($dest))
            if ($file.existed) {
                $old = Join-Path $backup $itemName
                [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($old))
                [IO.File]::Move($dest, $old)
            }
            [IO.File]::Move((Join-Path $stage $itemName), $dest)
        }
        [IO.File]::WriteAllText($applied, '')
        Save-Owned
        [IO.File]::WriteAllText($complete, '')
        $restartNeeded = $false
        $step = '4/4 重启并检查进程'
        Report "[$step] 尝试启动 $root 的进程"
        Invoke-ProcessStep start
    } elseif (![IO.Directory]::Exists($pending)) {
        throw '没有待处理升级'
    } elseif ($Action -eq 'confirm') {
        $step = '提交验收'
        if (![IO.File]::Exists($applied)) { throw '尚未完成替换；保持停机并执行 rollback' }
        Save-Owned
        Finish-Transaction
        Report '已提交并清理本次备份，可以恢复节点接单。'
    } elseif ([IO.File]::Exists($journal) -or [IO.File]::Exists($restart)) {
        $step = '回滚'
        Recover-Instance
    } else {
        Finish-Transaction
        Report '已取消未写入安装目录的升级。'
    }
} catch {
    Report "失败 [$step] $itemName $($_.Exception.Message)"
    if ($restartNeeded) {
        Report '升级中断，正在恢复原版本及服务。'
        try { Recover-Instance } catch { Report "回滚未完成：$($_.Exception.Message)。保留 $pending，排除占用或权限问题后再次 rollback。" }
    } elseif ($preparedHere -and ![IO.File]::Exists($journal)) {
        Finish-Transaction
    }
    exit 1
} finally {
    if ($guard) { $guard.Dispose() }
}
exit 0

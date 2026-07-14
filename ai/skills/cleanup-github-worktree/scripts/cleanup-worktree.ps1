[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [int]$Days
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 4; $i++) { $dir = Split-Path -Parent $dir }
    return $dir
}

function Get-AllWorktrees {
    $lines = & git worktree list --porcelain
    if ($LASTEXITCODE -ne 0) { throw "Failed to list worktrees." }

    $worktrees = @()
    $currentPath = $null
    $currentBranch = $null
    $isBare = $false

    foreach ($line in $lines) {
        if ($line.StartsWith("worktree ")) {
            if ($null -ne $currentPath) {
                $worktrees += [PSCustomObject]@{ Path = $currentPath; Branch = $currentBranch; Bare = $isBare }
            }
            $currentPath = $line.Substring(9).Trim()
            $currentBranch = $null
            $isBare = $false
        } elseif ($line.StartsWith("branch refs/heads/")) {
            $currentBranch = $line.Substring(18).Trim()
        } elseif ($line -eq "bare") {
            $isBare = $true
        }
    }
    if ($null -ne $currentPath) {
        $worktrees += [PSCustomObject]@{ Path = $currentPath; Branch = $currentBranch; Bare = $isBare }
    }
    return $worktrees
}

$repoRoot = Get-RepoRoot
Set-Location $repoRoot

$cutoff = (Get-Date).AddDays(-$Days)
$worktrees = Get-AllWorktrees

function Get-BranchLastCommitTime {
    param([string]$BranchName, [string]$WorktreePath)

    if (-not [string]::IsNullOrWhiteSpace($BranchName)) {
        $ts = & git log -1 --format=%ct $BranchName 2>$null
        if ($LASTEXITCODE -eq 0 -and $ts -match '^\d+$') {
            return ([DateTimeOffset]::FromUnixTimeSeconds([long]$ts)).LocalDateTime
        }
    }
    # detached HEAD: fall back to directory mtime
    if (Test-Path -LiteralPath $WorktreePath) {
        return (Get-Item -LiteralPath $WorktreePath).LastWriteTime
    }
    return $null
}

$candidates = @()
$skippedFirst = $false
foreach ($wt in $worktrees) {
    if (-not $skippedFirst) {
        $skippedFirst = $true
        continue
    }
    if ($wt.Bare) { continue }
    if (-not (Test-Path -LiteralPath $wt.Path)) { continue }

    $lastActivity = Get-BranchLastCommitTime -BranchName $wt.Branch -WorktreePath $wt.Path
    if ($null -ne $lastActivity -and $lastActivity -lt $cutoff) {
        $candidates += [PSCustomObject]@{
            Path   = $wt.Path
            Branch = $wt.Branch
        }
    }
}

if ($candidates.Count -eq 0) {
    Write-Host "No stale worktrees found (threshold: $Days days)."
    exit 0
}

foreach ($c in $candidates) {
    Write-Host $c.Branch
}

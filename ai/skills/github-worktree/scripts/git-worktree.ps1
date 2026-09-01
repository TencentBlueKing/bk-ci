[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("Add", "List", "Remove")]
    [string]$Action,

    [string]$Branch,

    [string]$Path,

    [string]$Base = "master",

    [string]$Root,

    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    # scripts -> github-worktree -> skills -> .cursor -> repo root
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 4; $i++) { $dir = Split-Path -Parent $dir }
    return $dir
}

function Get-DefaultWorktreeRoot {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $repoParent = Split-Path -Parent $RepoRoot
    $repoName = Split-Path -Leaf $RepoRoot
    return (Join-Path $repoParent ($repoName + "-wt"))
}

function Get-BranchPathName {
    param([Parameter(Mandatory = $true)][string]$BranchName)

    return (($BranchName -replace '[\\/:*?"<>|]+', '-') -replace '\s+', '-')
}

function Resolve-TargetWorktreePath {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [string]$BranchName,
        [string]$ExplicitPath,
        [string]$ExplicitRoot
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitPath)) {
        return [System.IO.Path]::GetFullPath($ExplicitPath)
    }
    if ([string]::IsNullOrWhiteSpace($BranchName)) {
        throw "Branch is required when Path is not provided."
    }

    $worktreeRoot = if ([string]::IsNullOrWhiteSpace($ExplicitRoot)) {
        Get-DefaultWorktreeRoot -RepoRoot $RepoRoot
    } else {
        [System.IO.Path]::GetFullPath($ExplicitRoot)
    }
    return (Join-Path $worktreeRoot (Get-BranchPathName -BranchName $BranchName))
}

function Invoke-Git {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    Write-Host ("git " + ($Arguments -join " "))
    & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git command failed."
    }
}

function Test-LocalBranchExists {
    param([Parameter(Mandatory = $true)][string]$BranchName)

    & git show-ref --verify --quiet ("refs/heads/" + $BranchName)
    return ($LASTEXITCODE -eq 0)
}

function Get-WorktreePathByBranch {
    param([Parameter(Mandatory = $true)][string]$BranchName)

    $lines = & git worktree list --porcelain
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read worktree list."
    }

    $currentPath = $null
    foreach ($line in $lines) {
        if ($line.StartsWith("worktree ")) {
            $currentPath = $line.Substring(9).Trim()
            continue
        }
        if ($line.StartsWith("branch refs/heads/")) {
            $currentBranch = $line.Substring(18).Trim()
            if ($currentBranch -eq $BranchName) {
                return $currentPath
            }
        }
    }
    return $null
}

function Sync-ConfigDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$TargetPath,
        [Parameter(Mandatory = $true)][string]$DirectoryName,
        [Parameter(Mandatory = $true)][string]$DisplayName
    )

    $sourceDirectory = Join-Path $RepoRoot $DirectoryName
    if (-not (Test-Path -LiteralPath $sourceDirectory -PathType Container)) {
        return
    }

    $targetDirectory = Join-Path $TargetPath $DirectoryName
    if (Test-Path -LiteralPath $targetDirectory) {
        Remove-Item -LiteralPath $targetDirectory -Recurse -Force
    }
    Copy-Item -LiteralPath $sourceDirectory -Destination $targetDirectory -Recurse -Force
    Write-Host ("Synced " + $DisplayName + ": " + $targetDirectory)
}

function Invoke-AllJooq {
    param([Parameter(Mandatory = $true)][string]$TargetPath)

    $backendDir = Join-Path $TargetPath "src/backend/ci"
    $gradlew = Join-Path $backendDir "gradlew.bat"
    if (-not (Test-Path -LiteralPath $gradlew -PathType Leaf)) {
        Write-Warning ("Skip JOOQ: gradlew.bat not found at " + $gradlew)
        return
    }

    $tasks = @(
        "generateGenenrateJooq",
        "generateArtifactoryGenenrateJooq",
        "generateDispatchGenenrateJooq",
        "generateDispatch_kubernetesGenenrateJooq",
        "generateEnvironmentGenenrateJooq",
        "generatePluginGenenrateJooq",
        "generateProcessGenenrateJooq",
        "generateProjectGenenrateJooq",
        "generateQualityGenenrateJooq",
        "generateRepositoryGenenrateJooq"
    )

    Write-Host ("Running all JOOQ tasks in: " + $backendDir)
    Push-Location $backendDir
    try {
        & $gradlew @tasks "--console=plain"
        if ($LASTEXITCODE -ne 0) {
            throw "JOOQ generation failed."
        }
    } finally {
        Pop-Location
    }
}

$repoRoot = Get-RepoRoot
Set-Location $repoRoot

switch ($Action) {
    "Add" {
        if ([string]::IsNullOrWhiteSpace($Branch)) {
            throw "Add requires -Branch."
        }

        $targetPath = Resolve-TargetWorktreePath -RepoRoot $repoRoot -BranchName $Branch -ExplicitPath $Path -ExplicitRoot $Root
        $targetParent = Split-Path -Parent $targetPath
        if (-not (Test-Path -LiteralPath $targetParent -PathType Container)) {
            New-Item -ItemType Directory -Path $targetParent -Force | Out-Null
        }

        if (Test-LocalBranchExists -BranchName $Branch) {
            throw (
                "Local branch '$Branch' already exists. The Add action always creates a new branch " +
                "together with the worktree."
            )
        }

        Invoke-Git -Arguments @("worktree", "add", "-b", $Branch, $targetPath, $Base)

        Sync-ConfigDirectory `
            -RepoRoot $repoRoot `
            -TargetPath $targetPath `
            -DirectoryName ".idea" `
            -DisplayName "IDEA config"
        Sync-ConfigDirectory `
            -RepoRoot $repoRoot `
            -TargetPath $targetPath `
            -DirectoryName ".cursor" `
            -DisplayName "Cursor config"
        Write-Host ("Created worktree: " + $targetPath)
        Invoke-AllJooq -TargetPath $targetPath
        break
    }
    "List" {
        Invoke-Git -Arguments @("worktree", "list")
        break
    }
    "Remove" {
        $targetPath = $null
        if (-not [string]::IsNullOrWhiteSpace($Path)) {
            $targetPath = [System.IO.Path]::GetFullPath($Path)
        } elseif (-not [string]::IsNullOrWhiteSpace($Branch)) {
            $targetPath = Get-WorktreePathByBranch -BranchName $Branch
            if ([string]::IsNullOrWhiteSpace($targetPath)) {
                $targetPath = Resolve-TargetWorktreePath -RepoRoot $repoRoot -BranchName $Branch -ExplicitPath $null -ExplicitRoot $Root
            }
        } else {
            throw "Remove requires -Branch or -Path."
        }

        $arguments = @("worktree", "remove")
        if ($Force) {
            $arguments += "--force"
        }
        $arguments += $targetPath
        Invoke-Git -Arguments $arguments
        Write-Host ("Removed worktree: " + $targetPath)
        break
    }
}

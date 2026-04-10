# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BK-CI Agent is a Go-based CI/CD build agent for the BlueKing CI (蓝鲸持续集成) platform. It communicates with BK-CI backend to receive and execute build tasks, supports both binary (Java worker) and Docker/Podman container builds, and handles self-upgrade. Cross-platform: Linux, macOS, Windows, with ARM64/MIPS64/LoongArch variants.

**Go version**: 1.19 (locked, do NOT run `go mod tidy` as it upgrades to 1.21+)  
**Module**: `github.com/TencentBlueKing/bk-ci/agent`

## Build & Test Commands

```bash
# Build (output to bin/)
make build_linux          # Linux amd64
make build_linux_arm64    # Linux ARM64
make build_windows        # Windows amd64
make build_macos_no_cgo   # macOS (cross-compile friendly, no CGO)
make build_macos          # macOS with CGO (needed for cpu/diskio telegraf metrics)
make all                  # All platforms

# Specify Go binary
make build_linux GO=go1.19

# Test
make test                 # All unit tests
GO111MODULE=on go test -tags=out -run . ./...                    # Same as make test
GO111MODULE=on go test -tags=out -run TestFuncName ./src/pkg/... # Single test
GO111MODULE=on go test -tags=out -v ./src/pkg/config/...         # Single package

# Format
make format               # goimports + gofmt

# i18n code generation
make i18nfilegen
```

Build tag `-tags=out` is required for external release builds (BUILD_OUT_TAG=out).

Version is injected via ldflags: `AGENT_VERSION=v1.0.0 make build_linux`

## Architecture

### Three Binaries

- **devopsAgent** - Main process: polls backend for tasks, launches worker processes, reports results
- **devopsDaemon** - Watchdog: keeps Agent alive (Windows: SCM service via kardianos/service; Unix: 5s file-lock check timer)
- **upgrader** - Replaces agent/daemon binaries during self-upgrade

### Agent Main Loop (src/pkg/agent/)

1. CheckProcess (file lock in runtime/ directory)
2. Init config from `.agent.properties`, TLS certs, environment
3. Report startup to backend
4. Start concurrent goroutines: heartbeat (10s), upgrade check (20s), pipeline tasks (30s), cleanup (2h), data collection (telegraf)
5. **Main loop (5s)**: `api.GetBuild()` -> dispatch build task via `safeGo()`

### Key Packages (src/pkg/)

| Package | Purpose |
|---------|---------|
| `agent` | Main loop, heartbeat, Ask polling |
| `api` | All HTTP communication with BK-CI backend (auth headers: X-DEVOPS-PROJECT-ID/AGENT-ID/AGENT-SECRET-KEY) |
| `config` | `.agent.properties` INI parsing, GAgentConfig global, GAgentEnv, EBus event bus |
| `job` | Binary build execution: check worker.jar, create build_tmp, launch Java process, wait, report |
| `job_docker` | Docker/Podman build execution: pull image, create/start/wait container |
| `dockercli` | Docker/Podman CLI wrapper |
| `upgrade` | Self-upgrade: download with MD5 validation, file replacement, upgrader coordination via total-lock |
| `mcp` | MCP Server (Streamable HTTP on 127.0.0.1, env DEVOPS_AGENT_ENABLE_MCP=true) |
| `collector` | Telegraf-based monitoring data collection |
| `pipeline` | Pipeline script task execution |
| `cron` | Periodic cleanup tasks |
| `i18n` | Internationalization with code generation |

### Entry Points (src/cmd/)

- `src/cmd/agent/main.go` - devopsAgent
- `src/cmd/daemon/` - devopsDaemon (platform-specific implementations)
- `src/cmd/upgrader/main.go` - upgrader binary
- `src/cmd/translation_generator/` - i18n code generator

## Cross-Platform Conventions

### File Naming for Build Tags

- `*_win.go` / `*_windows.go` - Windows only
- `*_unix.go` - Linux + macOS (build tag `//go:build linux || darwin`)
- `*_linux.go` - Linux only
- `*_darwin.go` - macOS only

### Key Platform Differences

- **Process management**: Unix uses process groups (Setpgid); Windows uses Job Objects
- **Build launch**: Unix creates shell scripts with `exec -l` for login environment; Windows calls java directly
- **User switching**: Unix supports running builds as different users; Windows does not
- **Environment variables**: Unix reads os.Environ(); Windows polls registry every 3s
- **Docker builds**: Unix only
- **File replacement during upgrade**: Unix uses signals; Windows uses wait+retry for locked files
- **Daemon**: Unix uses file-lock polling; Windows uses SCM service with crash recovery

## Code Patterns

- **Goroutines**: Always use `safeGo("name", fn)` which wraps with `defer recover()`
- **Errors**: `github.com/pkg/errors` - use `errors.New()`, `errors.Wrap(err, "context")`
- **Logging**: `src/pkg/common/logs` package - `logs.Info()`, `logs.Warn()`, `logs.Error()`, `logs.WithError(err).Error()`
- **Concurrency**: `BuildTotalManager.Lock` for global build mutex, `atomic.Int32` for counters, `sync.Map` for concurrent state
- **Event bus**: `config.EBus.Publish()` / `Subscribe()` for decoupled component communication
- **File operations**: Use `exiterror.WriteFileWithCheck()` for safe file writes

## Runtime Directory Layout

```
{workDir}/
├── .agent.properties    # Main config (INI format)
├── .cert                # TLS certificate (optional)
├── devopsAgent[.exe]
├── devopsDaemon[.exe]
├── worker-agent.jar     # Java worker binary
├── runtime/             # PID/lock files
├── logs/                # devopsAgent.log
├── build_tmp/           # Per-build temp dirs
├── docker_build_tmp/    # Docker build workspace
├── tmp/                 # Upgrade temp files
└── jdk/, jdk17/         # JDK installations
```

## Key Environment Variables

| Variable | Purpose |
|----------|---------|
| `DEVOPS_AGENT_ENABLE_MCP` | Enable MCP Server (true/false) |
| `DEVOPS_AGENT_CONTAINER_RUNTIME` | `docker` (default) or `podman` |
| `DEVOPS_AGENT_DOCKER_CAP_ADD` | Additional Docker capabilities |
| `DEVOPS_AGENT_ENABLE_NEW_CONSOLE` | Windows: new console for builds |
| `DEVOPS_AGENT_ENABLE_EXIT_GROUP` | Process group fallback |
| `DEVOPS_AGENT_TIMEOUT_EXIT_TIME` | Consecutive timeout threshold before restart |

## Important Constraints

- **Go 1.19 locked**: Do NOT use `go mod tidy` (it upgrades go.mod to 1.21+). Use `GOFLAGS=-mod=mod go build` if needed.
- **telegraf pinned to v1.24.4**: Higher versions introduce memcall/memguard causing ulimit coredump=0 and ARM64 panics.
- **CGO required for macOS metrics**: telegraf cpu/diskio plugins need CGO on darwin. Use `build_macos_no_cgo` targets for cross-compilation.

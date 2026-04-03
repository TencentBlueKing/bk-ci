---
name: bk-ci-agent-dev
description: 蓝鲸CI Agent（Go语言）项目开发迭代指南。包含项目架构、模块职责、构建流程、平台适配、API通信、升级机制等完整知识。当用户修改Agent代码、修复bug、添加功能、理解构建流程、处理跨平台问题、调试Docker构建、分析升级逻辑时使用。
---

# BK-CI Agent 开发迭代指南

蓝鲸CI构建机Agent，Go语言编写，负责与BK-CI后台通信、接取并执行构建任务、自升级、Docker构建、镜像调试等。

## 触发条件

- 修改或新增 Agent 功能代码
- 修复 Agent bug
- 理解 Agent 架构和工作流程
- 处理跨平台（Windows/Linux/macOS）兼容性
- Docker 构建相关开发
- 升级机制开发
- API 通信相关修改
- 配置项变更

## 项目概览

```
src/agent/agent/
├── src/
│   ├── cmd/                    # 入口程序（4个可执行文件）
│   │   ├── agent/main.go       # Agent 主程序
│   │   ├── daemon/main.go      # 守护进程（保活Agent）
│   │   ├── upgrader/main.go    # 升级器（替换二进制）
│   │   └── installer/main.go   # 安装器（首次安装）
│   ├── pkg/                    # 核心业务包
│   │   ├── agent/              # Agent主循环与任务分发
│   │   ├── api/                # 与BK-CI后台HTTP API通信
│   │   ├── config/             # 配置加载与管理
│   │   ├── job/                # 构建任务执行（核心）
│   │   ├── job_docker/         # Docker CLI 参数转换与策略判断
│   │   ├── dockercli/          # Docker/Podman CLI 运行层（命令执行/拉镜像/创建/等待/日志）
│   │   ├── upgrade/            # 升级流程管理
│   │   ├── upgrader/           # upgrader进程逻辑
│   │   ├── installer/          # installer进程逻辑
│   │   ├── imagedebug/         # Docker镜像调试
│   │   ├── mcp/                # MCP Server（Streamable HTTP，使用 go-mcp SDK）
│   │   ├── pipeline/           # 流水线引擎(实验性)
│   │   ├── envs/               # 环境变量管理
│   │   ├── cron/               # 定时任务
│   │   ├── collector/          # 系统信息采集(Telegraf)
│   │   ├── exiterror/          # 结构化退出错误
│   │   ├── i18n/               # 国际化
│   │   ├── constant/           # 常量(内外版差异+环境变量开关)
│   │   └── util/               # 工具包
│   │       ├── command/        # 命令执行与用户切换
│   │       ├── fileutil/       # 文件操作
│   │       ├── httputil/       # HTTP客户端
│   │       ├── process/        # 进程管理(Windows Job Object)
│   │       ├── systemutil/     # 系统工具(路径/权限/目录)
│   │       └── wintask/        # Windows服务检测
│   └── third_components/       # 第三方组件管理(JDK/Worker)
├── internal/
│   └── third_party/dep/fs/     # 文件系统操作(跨设备rename)
├── other-utils/
│   └── process-tree/           # 独立诊断工具 `agent-util`：Windows `tree` + Unix `shell-check`，含 `build.sh`
├── Makefile                    # 多平台构建
├── build_windows.ps1           # Windows构建脚本
├── go.mod                      # Go 1.19, 即将升级到 1.22
└── bin/                        # 编译产物输出
```

## 核心架构

### Agent 主循环 (`pkg/agent/`)

```
agent.Run()
  ├─ 初始化: config加载 → 环境变量 → TLS证书 → 定时任务
  ├─ 启动 heartbeat goroutine (每30秒)
  ├─ mcp.SyncState()        // 检查环境变量，启用则在 goroutine 中启动 MCP Server（Streamable HTTP 随机端口），心跳中动态启停
  └─ 主循环 (每5秒):
       api.Ask() → 解析响应 → doAgentJob():
         ├─ HeartInfo   → safeGo(agentHeartbeat)     // 心跳
         ├─ BuildInfo   → safeGo(job.DoBuild)         // 执行构建
         ├─ UpgradeItem → safeGo(upgrade.AgentUpgrade) // 升级
         ├─ PipelineData→ safeGo(pipeline.RunPipeline) // 流水线
         └─ DebugInfo   → safeGo(imagedebug.DoDebug)   // 镜像调试
```

**safeGo**: 所有异步任务通过 `safeGo(name, fn)` 启动，内含 `defer recover()` 防止 panic 崩溃进程。

**关键文件**:
- `agent.go` — 主循环、初始化、Ask轮询、safeGo、MCP启动
- `ask.go` — 处理轮询返回的不同任务类型
- `heartbeat.go` — 心跳上报(Agent状态、版本、系统信息)

### API 通信 (`pkg/api/`)

所有与BK-CI后台的通信都在此包。使用HTTP请求，鉴权通过Header传递。

**核心函数**:
| 函数 | 说明 |
|------|------|
| `GetBuild()` | 轮询获取构建任务 |
| `WorkerBuildFinish()` | 上报构建完成 |
| `GetAgentStatus()` | 获取Agent状态(心跳) |
| `UploadPipelineData()` | 上报流水线执行数据 |
| `DownloadUpgradeFile()` | 下载升级文件 |
| `FinishUpgrade()` | 上报升级完成 |
| `DownloadDockerInitFile()` | 下载Docker初始化脚本 |

**鉴权Header**: `X-DEVOPS-PROJECT-ID`, `X-DEVOPS-AGENT-ID`, `X-DEVOPS-AGENT-SECRET-KEY`

**重要**: HTTP客户端有超时重试机制，连续超时达阈值会触发Agent退出重启；代理请求支持 `HTTP_PROXY` / `HTTPS_PROXY` / `NO_PROXY`，并会优先读取后台下发后持久化到 `.agent.properties` 的代理配置。

### 构建任务执行 (`pkg/job/`)

这是最核心的包，处理二进制构建和Docker构建两种模式。

#### 二进制构建流程:
```
DoBuild(buildInfo)
  → acquireBuildSlot()               // 子函数，内部 Lock + defer Unlock
       → CheckParallelTaskCount()    // 检查并行数
       → 返回 buildTypeDocker/buildTypeNormal/buildTypeNone
  → i18n.CheckLocalizer()           // 接取任务后检查语言
  → switch buildType:
       docker → runDockerBuild()
       normal → runBuild()
            → 检查 worker-agent.jar 存在
            → 创建 build_tmp/{buildId}_{vmSeqId}/ 目录
            → 预写 build_msg.log("BuilderProcessWasKilled")
            → doBuild() [平台特定]
                 Unix:  写启动Shell脚本 → exec /bin/bash → java -jar worker-agent.jar
                 Windows: 直接 java -jar worker-agent.jar
            → cmd.Wait() 等待完成
            → 读取 build_msg.log (异常消息)
            → api.WorkerBuildFinish() 上报
            → 清理临时文件
```

#### Docker构建流程:
```
runDockerBuild(buildInfo)
  → dockercli.Runner 创建 (默认 docker，可通过环境变量切换 podman)
  → 镜像拉取(根据策略)
  → 组装 `docker create` 参数:
       挂载: JRE(只读), worker-agent.jar(只读), init.sh(只读), workspace(读写), logs(读写)
       环境变量: project_id, agent_id, secret_key, gateway, build_env=DOCKER
       CapAdd: SYS_PTRACE
       网络: bridge
  → 执行 `docker create`
  → 执行 `docker start`
  → 执行 `docker wait`
  → 执行 `docker logs` 读取容器日志
  → dockerBuildFinish() 上报
```

**运行时切换**:
- 默认使用 `docker` 命令行
- 通过环境变量 `DEVOPS_AGENT_CONTAINER_RUNTIME=podman` 可切换为 `podman`
- 所有核心容器生命周期操作（pull/create/start/wait/logs/rm/stop/ps）统一走 `pkg/dockercli/`
- 会将完整命令、stdout、stderr 打到 Agent 日志；构建任务还会将关键命令输出上报到构建日志

**关键文件**:
- `build.go` — runBuild()、workerBuildFinish()
- `build_docker.go` — runDockerBuild()、Docker/Podman CLI 运行逻辑
- `docker_runtime.go` — 构建容器参数拼装（create/mount/env）
- `build_manager.go` — BuildManager管理并发构建实例
- `do_build.go` — Unix平台doBuild()实现
- `do_build_win.go` — Windows平台doBuild()实现

### 配置系统 (`pkg/config/`)

**配置文件**: `.agent.properties` (INI格式)

```properties
# 必填项
devops.project.id=项目ID
devops.agent.id=Agent唯一ID
devops.agent.secret.key=鉴权密钥
landun.gateway=网关地址
landun.fileGateway=文件网关地址

# 可选项(有默认值)
devops.parallel.task.count=4            # 并行构建数
devops.agent.request.timeout.sec=5      # HTTP超时
devops.agent.logs.keep.hours=96         # 日志保留小时
devops.docker.parallel.task.count=4     # Docker并行数
devops.docker.enable=true               # 启用Docker构建
devops.slave.user=                      # 构建运行用户(Unix)
devops.agent.detect.shell=false         # 检测Shell
devops.language=zh_CN                   # 语言
devops.imagedebug.portrange=30000-32767 # 调试端口范围
```

**全局配置对象**: `config.GAgentConfig` — 所有模块直接引用

**事件总线**: `config.EventBus` — 发布订阅模式，目前仅 `IpEvent`(IP变更通知)

### 升级机制 (`pkg/upgrade/`)

```
AgentUpgrade(upgradeItem, hasBuild)
  → 检查无运行中构建
  → downloadUpgradeFiles()  // 下载到 tmp/
       ├─ Agent二进制 + Daemon + Upgrader (MD5比对)
       ├─ worker-agent.jar (MD5比对)
       ├─ jdk17.zip
       └─ agent_docker_init.sh
  → DoUpgradeOperation()
       ├─ JDK: 解压 → 更新路径 → 异步删旧
       ├─ Worker: 复制JAR → 刷新版本号
       ├─ Agent: 启动upgrader进程 → 替换二进制 → 重启
       └─ Docker Init: 复制 + 设权限
  → api.FinishUpgrade(success)
```

**升级器** (`pkg/upgrader/`): 独立进程，负责替换Agent/Daemon二进制文件并重启。平台差异大——Unix使用文件替换+进程信号，Windows需要等待进程退出再替换。

## 跨平台开发指南

### 平台文件命名约定

| 后缀 | 平台 | 示例 |
|------|------|------|
| `_win.go` | Windows | `do_build_win.go`, `config_win.go` |
| `_unix.go` | Linux + macOS | `download_unix.go` |
| `_linux.go` | 仅Linux | `config_linux.go` |
| `_darwin.go` | 仅macOS | `config_darwin.go`, `download_darwin.go` |
| `_nowin.go` | 非Windows | `env_polling_nowin.go` |

### Build Tags

```go
//go:build windows           // Windows专用
//go:build linux              // Linux专用
//go:build darwin             // macOS专用
//go:build linux || darwin    // Unix通用
//go:build !windows           // 非Windows
//go:build !out               // 内部版本(默认)
//go:build out                // 外部版本
```

**补充说明**: `other-utils/process-tree/` 目录下的工具对外统一命名为 `agent-util`，采用同目录多入口模式：`main.go` 仅在 Windows 编译，承载 `tree` 子命令；`main_unix.go` + `shell_check_unix.go` 在 Linux/macOS 编译，承载 `shell-check` 子命令；`build.sh` 负责一键构建 `linux/windows` 两个平台产物。

### 关键平台差异

| 功能 | Unix | Windows |
|------|------|---------|
| **进程管理** | `Setpgid` + `syscall.Kill(-pgId)` | Windows Job Object (`JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`) |
| **构建启动** | 写Shell脚本 → `/bin/bash` 执行 | 直接 `java -jar` |
| **用户切换** | `syscall.Credential{Uid, Gid}` | 不支持(空操作) |
| **环境变量** | `os.Environ()` | 注册表轮询(每3秒) + PATH合并 |
| **Docker** | 完整支持 | 不支持 |
| **文件权限** | `os.Chmod` | 空操作 |
| **进程替换** | 文件替换 + 进程信号 | 等待退出 + 文件替换 |
| **硬件信息采集** | Linux/Windows 可走通用 `ghw` 采集；macOS 需用 `_darwin.go` 单独兜底，当前跳过 GPU 采集以规避 `ghw` 未实现报错 | GPU 标签主要用于指标补充，不应影响 Agent 启动 |

### 安装模式 (`install --mode`)

所有平台的 `install` 命令通过 `--mode` 选择安装模式。如果目标模式与当前已安装模式相同则跳过；不同则自动先 `uninstall` 再安装。

| 平台 | 可选模式 | 默认 | 说明 |
|------|---------|------|------|
| **Linux** | `service` / `user` / `direct` | root: `service`; 非 root: `direct` | `service` = 系统级 systemd (`/etc/systemd/system/`)；`user` = 用户级 systemd (`~/.config/systemd/user/`, 需 `loginctl enable-linger`)；`direct` = 直接启动 |
| **macOS** | `login` / `background` | `login` | `login` = 需登录桌面, 直接启动；`background` = 无头模式 (`user/UID` 域 + `LimitLoadToSessionType=Background`) |
| **Windows** | `service` / `session` / `task` | `service` | `service` = Windows 服务；`session` = 服务 + 桌面会话；`task` = 已废弃计划任务 |

通过 `.install_type` 文件持久化当前模式，`start`/`stop` 自动检测并使用对应的启停方式。

### 内外版差异 (`constant/`)

通过 build tag `out` / `!out` 区分:
- **内部版** Docker数据目录: `/data/landun/workspace`
- **外部版** Docker数据目录: `/data/devops/workspace`
- HTTP错误检查逻辑也有差异

## 编码规范

### 错误处理模式

```go
// 1. 使用 github.com/pkg/errors 包装错误
return errors.New("load agent config failed")
return errors.Wrap(err, "context message")
return errors.Wrapf(err, "cannot rename %s to %s", src, dst)

// 2. ExitError 结构化退出 (pkg/exiterror/)
// 用于累计计数的异常检测，达到阈值触发Agent退出重启
exiterror.CheckOsIoError(name, err)  // 检查 ENOSPC, EACCES

// 3. IO操作使用安全写入
exiterror.WriteFileWithCheck(name, data, perm)
```

### 日志模式

```go
import "github.com/TencentBlueKing/bk-ci/agentcommon/logs"

logs.Info("message")
logs.Infof("format %s", arg)
logs.Warn("message")
logs.Error("message")
logs.WithError(err).Error("context")
logs.WithErrorNoStack(err).Error("context")  // 不打印堆栈
```

### 并发控制模式

```go
// 1. safeGo — 所有异步任务统一使用，防止 panic 崩溃进程
safeGo("taskName", func() { doSomething() })

// 2. 全局互斥锁 — 构建任务接取（通过 acquireBuildSlot 子函数，defer Unlock）
func acquireBuildSlot(buildInfo) buildSlotType {
    BuildTotalManager.Lock.Lock()
    defer BuildTotalManager.Lock.Unlock()
    // ...判断并发数并注册实例
    return buildTypeDocker / buildTypeNormal / buildTypeNone
}

// 3. 原子计数 — 错误累计
var counter atomic.Int32
counter.Add(1)

// 4. sync.Map — 并发安全的构建实例管理
manager.instances.Store(buildId, instance)

// 5. EventBus — 带缓冲channel的发布订阅（全非阻塞 select 防竞争）
config.GAgentConfig.EventBus.Publish("IpEvent", data)

// 6. i18n RWMutex — Localize() 单次 RLock，getLocalizerLocked() 不再自行加锁
//    避免嵌套读锁导致写锁等待时死锁
```

**并发注意事项**:
- 锁操作务必使用 `defer Unlock()`，禁止手动多路径 Unlock
- 新增 goroutine 使用 `safeGo()` 包装，确保 panic 不会崩溃进程
- `i18n.Localize()` 只加一次 RLock，内部 `getLocalizerLocked()` 不再加锁
- EventBus `Publish` 使用全非阻塞 select 模式，丢弃和写入分两步各用 select 防止并发竞争

### MCP Server (`pkg/mcp/`)

MCP (Model Context Protocol) Server 作为 agent 主进程的一个协程运行，通过 Streamable HTTP 在本地端口监听，供外部 AI 工具连接。

**架构设计**:
- 使用第三方库 `github.com/ThinkInAIXYZ/go-mcp`（需要 Go 1.18+，兼容项目 go 1.19）
- 传输方式：Streamable HTTP（`POST /mcp` 端点），绑定 `127.0.0.1` 仅本机访问
- 端口持久化到 `.agent.properties` 的 `devops.mcp.server.port`，优先复用已有端口，不可用时随机分配新端口
- 通过环境变量 `DEVOPS_AGENT_ENABLE_MCP=true` 启用
- **支持动态启停**：`mcp.SyncState()` 在 `agent.Run()` 启动时和每次心跳环境变量更新后调用，检测开关变化自动启停 Server
- 停止时通过 `server.Shutdown()` 优雅关闭

**MCP 客户端配置**（CodeBuddy / Claude Desktop 等）:
```json
{
  "mcpServers": {
    "bk-ci-agent": {
      "url": "http://127.0.0.1:{PORT}/mcp"
    }
  }
}
```
其中 `{PORT}` 从 `.agent.properties` 中的 `devops.mcp.server.port` 读取。

**提供的 MCP Tool**:
| Tool | 说明 |
|------|------|
| `list_running_builds` | 获取所有运行中构建任务信息（普通构建+Docker构建） |
| `get_recent_error_logs` | 获取近期错误日志，支持 `level`(error/warn/all) 和 `lines` 参数 |

**关键文件**:
- `entry.go` — `SyncState()` 入口，动态检测环境变量变化并启停 MCP Server，维护 `running` 状态和 `stopFunc` 回调
- `server.go` — Streamable HTTP Server 创建、端口解析（配置端口不可用则启动失败）、注入 `Shutdown` 回调
- `tools.go` — Tool 注册（使用 `protocol.NewToolWithRawSchema`）、handler 实现、辅助函数

**扩展新 Tool**:
```go
// 1. 在 tools.go 的 registerAllTools() 中添加
newTool := protocol.NewToolWithRawSchema(
    "tool_name",
    "tool description",
    []byte(`{"type":"object","properties":{...}}`),
)
s.RegisterTool(newTool, handleNewTool)

// 2. 实现 handler 函数
func handleNewTool(ctx context.Context, req *protocol.CallToolRequest) (*protocol.CallToolResult, error) {
    // req.Arguments 获取参数
    return newTextResult("result text"), nil
}
```

**导出接口**（供 MCP tool 查询 agent 内部状态）:
- `job.GBuildManager.GetInstancesWithPid()` — 返回 `map[int]*ThirdPartyBuildInfo`（pid → 构建信息）
- `job.GBuildDockerManager.GetInstances()` — 返回 Docker 构建实例列表

### 新增功能检查清单

- [ ] 是否需要平台特定实现？需要的话创建 `_win.go` / `_unix.go` 等文件
- [ ] 是否需要在 `api/` 新增API调用？遵循现有 `buildUrl` + `request` 模式
- [ ] 是否需要新配置项？在 `config.go` 的 `AgentConfig` 添加字段并在 `LoadAgentConfig()` 解析
- [ ] 是否涉及文件操作？使用 `exiterror.WriteFileWithCheck()` 确保IO错误检测
- [ ] 是否需要定时执行？在 `cron/cron.go` 的 `InitCron()` 注册
- [ ] 是否涉及Docker？确保只在Linux编译(`//go:build linux || darwin`对应)
- [ ] 是否需要国际化？在 `i18n/` 添加翻译
- [ ] 新增 goroutine 是否使用了 `safeGo()` 包装？
- [ ] 锁操作是否使用了 `defer Unlock()`？禁止手动多路径 Unlock
- [ ] 是否需要通过 MCP 暴露新信息？在 `mcp/tools.go` 新增 Tool
- [ ] 新增环境变量开关？在 `constant/constant.go` 添加常量

## 构建与测试

### 本地构建

```bash
# Linux (amd64)
make build_linux

# Windows (当前386，计划升级amd64)
make build_windows

# macOS
make build_macos

# 全平台
make build_all
```

### 编译注入变量

通过 `-ldflags` 注入:
```
config.BuildTime    = 编译时间
config.GitCommit    = Git Commit
config.AgentVersion = 版本号(version.go中声明)
```

### 运行测试

```bash
go test ./...
```

**用户文档**（`doc/` 目录下，面向用户和 AI 查询）:
- `doc/README.md` — 文档索引总览，快速入门指引
- `doc/installation.md` — 安装部署指南（前置条件、各平台安装步骤、部署场景）
- `doc/agent-cli.md` — CLI 命令参考（install/uninstall/start/stop/status/reinstall/repair/debug/version）
- `doc/architecture.md` — 架构与模块说明（进程模型、主循环、模块清单）
- `doc/configuration.md` — 配置参考（`.agent.properties` 参数详解、辅助文件、环境变量开关）
- `doc/platform-linux.md` — Linux 平台指南（systemd 模式、Root/非 Root、容器部署、login shell）
- `doc/platform-macos.md` — macOS 平台指南（launchd 模式、无头模式、JDK 路径差异）
- `doc/platform-windows.md` — Windows 平台指南（服务/Session 模式、环境变量自动刷新）
- `doc/build-and-workspace.md` — 构建系统与工作空间（Worker、JDK、磁盘占用与清理）
- `doc/docker-builds.md` — Docker/Podman 容器构建（运行时切换、挂载规则、镜像调试）
- `doc/upgrade-and-reinstall.md` — 升级与重装机制（自动升级流程、reinstall 完全重装）
- `doc/troubleshooting.md` — 故障排查与状态检查（status 解读、健康检查、常见故障）

**人工测试文档**:
- `doc/manual-test-agent-features.md` — 单测无法覆盖的人工上机验证清单（CLI / reinstall / status / Docker/Podman / 镜像调试 / 旧脚本兼容）

### 关键第三方依赖

| 依赖 | 版本 | 用途 | 注意事项 |
|------|------|------|---------|
| `creack/pty` | v1.1.24 | 镜像调试 WebSocket exec 的 PTY 转发 | 与 `docker/podman exec -it` 组合使用，兼容 Go 1.19 |
| `influxdata/telegraf` | v1.24.4 | 系统指标采集 | 有版本限制，不可随意升级 |
| `shirou/gopsutil/v3` | v3.22.9 | 系统信息获取 | 与telegraf绑定 |
| `pkg/errors` | v0.9.1 | 错误包装 | 项目统一使用 |
| `go-ini/ini` | v1.67.0 | 配置文件解析 | .agent.properties |
| `nicksnyder/go-i18n/v2` | v2.4.1 | 国际化 | |
| `ThinkInAIXYZ/go-mcp` | v0.2.24 | MCP Server SDK (Streamable HTTP) | 需要 Go 1.18+，兼容 go 1.19 |

### Go版本升级注意（1.19→1.22）

- 自定义 `min`/`max` 函数会与内置冲突（本项目无此问题）
- `for range` 循环变量语义变化（每次迭代独立变量）
- 最低系统要求提升：Windows 10+, CentOS 7+(边缘)

## 常见开发场景

### 场景1: 新增一个API调用

```go
// 在 api/api.go 中添加
func NewApiCall(params Params) (*httputil.DevopsResult, error) {
    url := buildUrl(fmt.Sprintf("/ms/environment/api/buildAgent/agent/newEndpoint/%s", params.Id))
    body, _ := json.Marshal(params)
    return httputil.NewHttpClient().Post(url).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).
        Body(strings.NewReader(string(body))).
        SetTimeoutRate(30).  // 超时秒数
        Execute()
}
```

### 场景2: 新增配置项

1. 在 `config/config.go` 的 `AgentConfig` 结构体添加字段
2. 在 `LoadAgentConfig()` 中解析: `cfg.Section("").Key("devops.new.config").MustString("default")`
3. 如需持久化，在 `SaveConfig()` 中写回

### 场景3: 新增定时任务

```go
// 在 cron/cron.go 的 InitCron() 中添加
go func() {
    for {
        newCleanJob()
        time.Sleep(2 * time.Hour)
    }
}()
```

### 场景4: 添加Docker容器配置

在 `job/build_docker.go` 的 `runDockerBuild()` 中修改容器创建配置:
- 挂载点在 `mounts` 切片
- 环境变量在 `parseContainerEnv()`
- CapAdd/网络在 `hostConfig`

## 文件与目录约定

### 运行时目录结构

```
{workDir}/                          # Agent工作目录
├── .agent.properties               # 主配置文件（含 MCP 端口 devops.mcp.server.port）
├── .cert                           # TLS证书(可选)
├── devopsAgent[.exe]               # Agent二进制
├── devopsDaemon[.exe]              # Daemon二进制
├── worker-agent.jar                # Worker JAR
├── agent_docker_init.sh            # Docker初始化脚本(Linux)
├── jdk/                            # JDK8
├── jdk17-{timestamp}/              # JDK17(带时间戳)
├── runtime/
│   ├── {name}.lock                 # 进程互斥锁
│   ├── {name}.pid                  # PID文件
│   └── total-lock.lock             # 全局总锁
├── tmp/                            # 升级下载临时目录
├── build_tmp/                      # 构建临时目录
│   └── {buildId}_{vmSeqId}_build_msg.log
├── logs/                           # 日志目录
└── docker_build_tmp/               # Docker构建工作空间
    ├── data/{pipelineId}/{vmSeqId}/
    └── logs/{buildId}/{vmSeqId}/
```

### 构建产物

```
bin/
├── devopsDaemon[.exe]
├── devopsAgent[.exe]
├── upgrader[.exe]
└── installer[.exe]
```

## 调试技巧

1. **Agent日志**: `{workDir}/logs/` 目录下的日志文件
2. **构建错误**: 检查 `build_tmp/{buildId}_{vmSeqId}_build_msg.log`
3. **Docker构建日志**: `docker_build_tmp/logs/{buildId}/{vmSeqId}/`
4. **HTTP通信调试**: 日志中搜索请求URL和响应状态码
5. **升级问题**: 检查 `tmp/` 目录下载的文件和MD5
6. **Windows服务问题**: 使用 `wintask` 包检测启动方式
7. **MCP调试**: 设置 `DEVOPS_AGENT_ENABLE_MCP=true`，agent 启动后 MCP Server 在 `127.0.0.1` 监听 Streamable HTTP，端口号持久化到 `.agent.properties` 的 `devops.mcp.server.port`。可在 CodeBuddy/Claude Desktop 中配置 `http://127.0.0.1:{PORT}/mcp` 进行交互式调试

## 环境变量开关

| 环境变量 | 默认 | 说明 |
|---------|------|------|
| `DEVOPS_AGENT_ENABLE_NEW_CONSOLE` | false | Windows 启动进程时使用 newConsole |
| `DEVOPS_AGENT_ENABLE_EXIT_GROUP` | false | 启动杀掉构建进程组的兜底逻辑 |
| `DEVOPS_AGENT_DOCKER_CAP_ADD` | 空 | Docker/Podman 启动容器时追加的 `--cap-add` 参数 |
| `DEVOPS_AGENT_CONTAINER_RUNTIME` | docker | 容器运行时命令，可切换为 `podman` |
| `DEVOPS_AGENT_TIMEOUT_EXIT_TIME` | 空 | 超时次数阈值，达到后Agent进程退出 |
| `DEVOPS_AGENT_ENABLE_MCP` | false | 随 agent 主进程启动 MCP Server 协程（Streamable HTTP） |

## 注意事项

1. **Go版本锁定**: `go.mod` 中 `go 1.19` 不可随意升级。引入新依赖时必须确认其最低Go版本要求兼容1.19。**禁止**使用 `go mod tidy`（Go 1.21+工具链会自动将go指令升级到1.21），应使用 `GOFLAGS=-mod=mod go build` 来自动更新 go.mod，或通过 gvm 切换到 Go 1.19 后再执行 `go mod tidy`
2. **`unsafe.Pointer` 使用**: `process_exit_group_win.go` 中通过 unsafe 读取 `os.Process` 内部字段，Go版本升级时需验证内存布局
3. **Telegraf版本锁定**: `go.mod` 中注释说明了 telegraf/gopsutil/docker 版本绑定关系，不可独立升级
4. **环境变量优先级**: API配置变量 > 系统环境变量
5. **构建成功等待**: 构建成功后会 `time.Sleep(8*time.Second)` 等待日志写入
6. **MD5缓存**: Docker Init文件的MD5有缓存机制，避免每次心跳重新计算

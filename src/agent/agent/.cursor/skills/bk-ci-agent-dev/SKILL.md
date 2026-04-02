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
│   │   ├── agent/main.go       # Agent 主程序（含 CLI 子命令入口）
│   │   ├── daemon/             # 守护进程（保活Agent）
│   │   │   ├── main.go         #   Linux/macOS 入口
│   │   │   ├── main_win.go     #   Windows 入口（kardianos/service）
│   │   │   └── session_windows.go # Windows用户会话启动（WTS API）
│   │   └── upgrader/main.go    # 升级器（替换二进制）
│   ├── pkg/
│   │   ├── agentcli/           # Agent CLI 子命令系统（含 i18n、状态检测、重装）
│   │   │   ├── cli.go          #   子命令路由、preserveSet、reinstall/repair 逻辑
│   │   │   ├── cli_test.go     #   通用函数测试
│   │   │   ├── i18n.go         #   国际化核心（msg/msgf/initLang/printUsage）
│   │   │   ├── i18n_test.go    #   i18n 测试
│   │   │   ├── i18n_nowin.go   #   非 Windows detectPlatformLang stub
│   │   │   ├── i18n_win.go     #   Windows UTF-8 控制台 + GetUserDefaultUILanguage
│   │   │   ├── service_linux.go #  Linux systemd/direct 服务管理
│   │   │   ├── service_darwin.go # macOS launchd 服务管理
│   │   │   ├── service_win.go  #   Windows sc.exe 服务管理
│   │   │   ├── status_linux.go #   Linux 状态检测（systemd/进程/JDK）
│   │   │   ├── status_linux_test.go # Linux 状态检测测试
│   │   │   ├── status_darwin.go #  macOS 状态检测（launchd/进程/JDK）
│   │   │   ├── status_win.go  #    Windows 状态检测（服务/LSA/AutoLogon/计划任务）
│   │   │   ├── session_win.go  #   Windows Session 配置（LSA/AutoLogon/凭据验证）
│   │   │   ├── session_win_test.go # Windows Session 测试
│   │   │   ├── session_nowin.go #  非Windows stub
│   │   │   ├── diagnose.go     #   健康检查核心（网络4步诊断/磁盘可写/证书/代理检测）
│   │   │   ├── diagnose_unix.go #  Linux/macOS 磁盘空间 (syscall.Statfs)
│   │   │   ├── diagnose_win.go #   Windows 磁盘空间 (GetDiskFreeSpaceEx)
│   │   │   └── diagnose_test.go #  健康检查测试
│   ├── pkg/                    # 核心业务包
│   │   ├── agent/              # Agent主循环与任务分发
│   │   ├── api/                # 与BK-CI后台HTTP API通信
│   │   ├── config/             # 配置加载与管理
│   │   ├── job/                # 构建任务执行（核心）
│   │   ├── job_docker/         # Docker CLI 参数转换与策略判断
│   │   ├── dockercli/          # Docker/Podman CLI 运行层（命令执行/拉镜像/创建/等待/日志）
│   │   ├── upgrade/            # 升级流程管理
│   │   ├── upgrader/           # upgrader进程逻辑
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

### Agent CLI 子命令系统 (`pkg/agentcli/`)

Agent 二进制 `devopsAgent` 同时作为 CLI 工具使用，通过 `main.go` 中的 `agentcli.IsSubcommand()` 判定后分流。

**入口分流规则**:
- 无参数: 正常启动 agent 主进程
- 有参数: 一律进入 CLI 分发
- 若参数不是已注册子命令: 直接输出 `unknown command` 和帮助信息并退出，**不会**再回落成启动 agent 主进程

**CLI 输出补充**:
- `status` 命令会在末尾输出一个汇总词条：全部检查正常时显示 `正常/Normal`，存在失败或告警时显示 `异常/Abnormal`
- Windows `configure-session` 完成后的摘要提示根据系统语言自动切换中英文；CLI 启动时会尝试将控制台切换为 UTF-8，尽量减少中文乱码
- `agentcli` 中所有面向用户的提示、警告和错误返回都应通过 `msg/msgf` 本地化；仅脚本内容、命令参数和系统原始输出可保持原文

**子命令一览**:

```
devopsAgent <command> [options]

服务管理:
  install [--mode <...>] [options]
                       安装并启动 Agent 守护进程
    Windows:
      --mode service     (默认) 安装为 Windows 服务
      --mode session     安装为 Windows 服务 + 配置桌面会话访问
      --mode task        [已废弃] 安装为计划任务
      --user USER        session 模式: Windows 登录账号
      --password PASS    密码 (--user 时必填)
      --auto-logon       配置 Windows 自动登录
    macOS:
      --mode login       (默认) 需要用户登录桌面, 直接启动进程
      --mode background  无需登录, SSH/无头可用 (类似 GitHub Actions Runner)
  uninstall            停止并卸载守护进程服务
  start                启动守护进程 (macOS 根据 .install_type 自动选择启动方式)
  stop                 停止守护进程 (macOS 根据 .install_type 自动选择停止方式)

维护:
  repair               修复文件: 停止 → 重新解压依赖 → 重启
  reinstall [-y]       完全重装: 内置下载 agent.zip (无需安装脚本)
  status               运行状态 + 健康检查 (网络/磁盘/证书诊断)

调试:
  debug [on|off]       切换调试模式 (通过 .debug 文件, 重启生效)
  version              打印版本号
    -f                 打印完整版本信息 (版本号 / Git Commit / 构建时间)

会话模式 (仅 Windows):
  configure-session    配置桌面会话访问 (也可通过 install --mode session 一步到位)
    --user USER        Windows 登录账号
    --password PASS    密码 (--user 时必填)
    --auto-logon       配置 Windows 自动登录
    --disable          取消会话模式

服务模式 (仅 macOS):
  configure-service    切换 launchd 服务模式 (也可通过 install --mode 一步到位)
    --mode login       切换为登录模式 (需要用户已登录桌面)
    --mode background  切换为后台模式 (无需登录, SSH/无头可用)
    --disable          恢复为登录模式
```

**i18n 机制** (`i18n.go`):
- `initLang()` 按优先级检测语言: `LANG` → `LC_ALL` → `LANGUAGE` → `.agent.properties` 中的 `devops.language` → 平台 API (Windows: `GetUserDefaultUILanguage`)
- 所有 CLI 输出通过 `msg(en, zh)` / `msgf(en, zh, args...)` 双语包装
- Windows 通过 `SetConsoleOutputCP(65001)` 确保 UTF-8 输出不乱码

**reinstall 流程**:
```
devopsAgent reinstall [-y]
  → 步骤 1: 下载 agent.zip（先验证，失败则不动本地文件）
  → 步骤 2: handleStop() 停止进程（不清除会话配置和 .install_type）
  → 步骤 3: 清理文件（保留 preserveSet 中的身份文件）
  → 步骤 4: 解压 agent.zip
  → 步骤 5: prepareWorkDir() 解压 JDK / 创建目录
  → 步骤 6: handleInstall() 重新注册服务
```
不依赖 Agent 内置升级机制，也不再依赖安装脚本；Agent 直接调用服务端接口下载 `agent.zip`。

**preserveSet** (重装时保留的文件):
| 文件 | 说明 |
|------|------|
| `.agent.properties` | Agent 身份配置 |
| `.install_type` | 安装模式标记 (Windows: SERVICE/SESSION, macOS: LOGIN/BACKGROUND) |
| `.cert` | TLS 证书信任文件 |
| `.debug` | Debug 模式标记文件 |
| `workspace/` | 构建工作空间 |
| `devopsAgent[.exe]` | 当前运行的 Agent 二进制 |

**macOS launchd 双模式服务管理**:
- 使用**现代 launchctl API** (macOS 10.10+): `bootstrap`/`bootout`/`kickstart` 替代已弃用的 `load`/`unload`
- **LOGIN 模式** (默认): 使用 `gui/UID` 域，无 `LimitLoadToSessionType`，`start`/`stop` 通过直接进程管理（类似旧 start.sh/stop.sh）
- **BACKGROUND 模式**: 使用 `user/UID` 域 + plist `LimitLoadToSessionType: Background`，`start`/`stop` 通过 `bootstrap`+`kickstart`/`bootout`（与 GitHub Actions Runner 一致）
- 通过 `.install_type` 文件 (`LOGIN`/`BACKGROUND`) 标记当前模式，`start`/`stop` 自动检测
- `install --mode login|background` 或 `configure-service --mode login|background` 切换模式
- root 用户始终使用 `system` 域 + `LaunchDaemons`

**status 命令** 输出内容 (按平台):
| 项目 | Linux | macOS | Windows |
|------|-------|-------|---------|
| 运行模式 | root+systemd / root+direct / non-root | LOGIN (gui/UID) / BACKGROUND (user/UID) | SERVICE / SESSION / TASK(legacy) |
| 服务状态 | systemctl is-active | plist 文件检测 | sc.exe query |
| 进程检测 | syscall.Kill(pid, 0) | syscall.Kill(pid, 0) | OpenProcess |
| 配置文件检查 | ini.Load 解析 + 必填项校验 | ini.Load 解析 + 必填项校验 | ini.Load 解析 + 必填项校验 |
| 会话详情 | — | — | LSA Secret 凭据 + AutoLogon 状态 |

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

**重要**: HTTP客户端有超时重试机制，连续超时达阈值会触发Agent退出重启。

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
       ├─ `Always` / 默认 `latest`: 直接 `pull`，跳过本地 `image inspect`
       └─ `IfNotPresent` / 默认非 `latest`: 先 `image inspect` 判断本地是否存在
  → 组装 `docker create` 参数:
       挂载: JRE(只读), worker-agent.jar(只读), init.sh(只读), workspace(读写), logs(读写)
       环境变量: project_id, agent_id, secret_key, gateway, build_env=DOCKER
       CapAdd: SYS_PTRACE
       网络: bridge (用户未指定 network 时)
  → 执行 `docker create`
  → 执行 `docker start`
  → 执行 `docker wait`
  → 执行 `docker logs` 读取容器日志
  → dockerBuildFinish() 上报

**Docker CLI 流程控制原则**（参考 GitHub Actions Runner `ContainerOperationProvider.cs`、Jenkins `DockerClient.java` 的 CLI 封装模式）:

所有 Docker/Podman CLI 交互 **只通过退出码和结构化输出控制流程**，禁止解析 stderr 文本做分支判断：

| 信息类型 | 获取方式 | 稳定性 |
|---------|---------|--------|
| 命令成功/失败 | 退出码 (`err != nil`) | 稳定 — 所有 Docker/Podman 版本一致 |
| 容器 ID | `docker create` stdout 最后一行 | 稳定 — Docker CLI 契约 |
| 容器退出码 | `docker wait` stdout 数字 | 稳定 — Docker CLI 契约 |
| 结构化查询 | `--format` Go template 输出 | 稳定 — 由模板语法控制，非自由文本 |
| 镜像是否存在 | `docker image inspect` 退出码 (0=存在) | 稳定 — 不解析 stderr |
| 日志级别分类 | stderr 中的 warning/deprecated 关键词 | 可接受 — 仅影响日志级别，不影响流程 |

**禁止**：通过 `strings.Contains(stderr, "xxx")` 判断镜像/容器/网络是否存在。stderr 文本因 Docker/Podman 版本、containerd 后端、操作系统语言等因素在不同环境下不一致。

**运行时切换**:
- 默认使用 `docker` 命令行
- 通过环境变量 `DEVOPS_AGENT_CONTAINER_RUNTIME=podman` 可切换为 `podman`
- 所有核心容器生命周期操作（pull/create/start/wait/logs/rm/stop/ps）统一走 `pkg/dockercli/`
- Docker CLI 日志按级别产生日志事件并同步到 Agent 日志 / 构建日志
- 命令本身: 普通命令记 `LOG`，`image inspect` 记 `DEBUG`
- 成功时 `stdout`: 记 `LOG`
- 成功时 `stderr`: 默认记 `LOG`，若包含 `warning` / `deprecated` 等提示则记 `WARN`
- 失败时 `stdout`: 记 `WARN`
- 失败时 `stderr`: 记 `ERROR`
- 构建日志接口路由:
  - `ERROR` 或显式 red: 走 `/ms/log/api/build/logs/red`
  - `WARN`: 走 `/ms/log/api/build/logs/yellow`
  - 其他级别: 走 `/ms/log/api/build/logs`
```

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

# Windows Session 凭据通过 LSA Secret 存储(configure_session.ps1 写入)
# 键名: BkCiSessionUser / BkCiSessionPassword，daemon 运行时读取
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

**Daemon-Upgrader 协调机制** (`total-lock`):
- Upgrader 在 `DoUpgradeAgent()` 中持有 `total-lock` 直到文件替换完成
- Linux/macOS daemon: `watch()` 每次检查/启动 agent 前都持有 `total-lock`，天然避免竞争
- Windows daemon: `watch()` 在启动 agent 前调用 `waitForUpgradeFinish()` 获取并立即释放 `total-lock`（gate 模式），确保 upgrader 不在运行
- Windows 文件替换: `replaceAgentFile()` 含重试机制（最多 10 次、递增间隔），防止因 Windows 文件锁（如杀毒软件扫描、系统休眠后 daemon 先于 upgrader 拉起 agent 导致 exe 被锁）导致 rename 失败

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

### 关键平台差异

| 功能 | Unix | Windows |
|------|------|---------|
| **进程管理** | `Setpgid` + `syscall.Kill(-pgId)` | Windows Job Object (`JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`) |
| **Daemon启动Agent** | `exec.Command` 直接启动 | 服务模式: WTS API `CreateProcessAsUser` 在用户Session启动; 交互模式: `exec.Command` 直接启动 |
| **构建启动** | 写Shell脚本 → `/bin/bash` 执行 | 直接 `java -jar` |
| **用户切换** | `syscall.Credential{Uid, Gid}` | 不支持(空操作) |
| **环境变量** | `os.Environ()` | 注册表轮询(每3秒) + PATH合并 |
| **Docker** | 完整支持 | 不支持 |
| **文件权限** | `os.Chmod` | 空操作 |
| **进程替换** | 文件替换 + 进程信号 | 等待退出 + 文件替换 |

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

// 3. 升级状态原子标志 — 防止升级期间接取构建任务
//    升级流程获得 Lock 并确认无运行中构建后置 true，升级完成后 defer 置 false
//    Ask 轮询中 checkBuildType / checkUpgrade 通过此标志跳过请求
BuildTotalManager.Upgrading.Store(true)  // 升级开始
BuildTotalManager.Upgrading.Load()       // Ask 阶段检查

// 4. 原子计数 — 错误累计
var counter atomic.Int32
counter.Add(1)

// 5. sync.Map — 并发安全的构建实例管理
manager.instances.Store(buildId, instance)

// 6. EventBus — 带缓冲channel的发布订阅（全非阻塞 select 防竞争）
config.GAgentConfig.EventBus.Publish("IpEvent", data)

// 7. i18n RWMutex — Localize() 单次 RLock，getLocalizerLocked() 不再自行加锁
//    避免嵌套读锁导致写锁等待时死锁
```

**并发注意事项**:
- 锁操作务必使用 `defer Unlock()`，禁止手动多路径 Unlock
- 新增 goroutine 使用 `safeGo()` 包装，确保 panic 不会崩溃进程
- `i18n.Localize()` 只加一次 RLock，内部 `getLocalizerLocked()` 不再加锁
- EventBus `Publish` 使用全非阻塞 select 模式，丢弃和写入分两步各用 select 防止并发竞争
- 升级期间通过 `BuildTotalManager.Upgrading` 原子标志阻止 Ask 轮询请求新构建任务，防止 Agent 二进制升级重启时阻塞中的构建丢失
- Daemon 启动 agent 前必须先通过 `total-lock` 确认无 upgrader 在运行（Linux/macOS 持有锁，Windows 使用 gate 模式），避免 daemon 在 upgrader 替换二进制期间拉起新 agent 导致文件锁冲突

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
- [ ] **测试用例**：新增/修改的可测试函数是否编写了单元测试？使用 table-driven + `t.Run()` 风格，平台特定代码需编写对应的 `_win_test.go` / `_unix_test.go`
- [ ] **功能文档**：是否需要更新 SKILL.md 中的架构描述、目录树、子命令说明？
- [ ] CLI 子命令新增/修改？更新 `agentcli/` 的 `IsSubcommand()`、`printUsageLocalized()` 和 SKILL.md

## 构建与测试

### 本地构建

```bash
# Linux (amd64)
make build_linux

# Windows (默认amd64)
make build_windows

# Windows (386, 带_386后缀)
make build_windows_386

# macOS
make build_macos

# 全平台
make all
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
# 全部测试
go test ./...

# 仅 agentcli 包（带详细输出）
go test -v ./src/pkg/agentcli/

# 运行特定测试
go test -v -run TestPidStatus ./src/pkg/agentcli/
```

**人工测试文档**:
- `doc/test/manual-test-agent-features.md` — 单测无法覆盖的人工上机验证清单（CLI / reinstall / status / Docker/Podman / 镜像调试 / 旧脚本兼容）

**测试规范**:

#### 基本原则
- **所有包含 Go 代码的目录都应尽可能编写单元测试**，新增/修改的可测试函数必须有对应测试
- 使用 table-driven + `t.Run()` 子测试风格（参考 `agentcli/cli_test.go`）
- 仅使用标准库 `testing`，手动断言 `if + t.Errorf/t.Fatalf`，不引入 testify 等第三方断言库
- 文件系统相关测试使用 `t.TempDir()`
- 全局状态（如 `useChinese`）测试前保存、测试后 `defer` 恢复

#### 日志初始化
- 凡是被测代码路径中调用了 `logs.Info/Error/...` 的测试文件，必须在 `init()` 中调用 `logs.UNTestDebugInit()`
- 同理，使用 `envs.FetchEnv` 的包需要 `envs.Init()`

```go
func init() {
    logs.UNTestDebugInit()
    envs.Init()
}
```

#### 平台特定测试
- **按编译条件为不同操作系统编写专属测试用例**，测试文件命名和 build tag 遵循以下约定:

| 测试文件命名 | Build Tag | 适用场景 |
|-------------|-----------|---------|
| `xxx_win_test.go` | `//go:build windows` | Windows 专属（Job Object、WTS Session、句柄继承、服务模式等） |
| `xxx_unix_test.go` | `//go:build linux \|\| darwin` | Unix 通用（Setpgid、信号处理、Shell 检测等） |
| `xxx_linux_test.go` | `//go:build linux` | 仅 Linux（Docker、cgroup、oom_score_adj 等） |
| `xxx_darwin_test.go` | `//go:build darwin` | 仅 macOS |
| `xxx_test.go` | 无 tag | 跨平台通用逻辑 |

- 同时使用旧格式 `// +build` 保持 Go 1.19 兼容:

```go
//go:build windows
// +build windows
```

#### 权限与环境敏感测试
- 需要管理员/SYSTEM 权限的测试（如 `WTSQueryUserToken`、`CreateProcessAsUser`），应在失败时使用 `t.Skipf()` 而非 `t.Fatalf()`
- 进程检测测试使用 `os.Getpid()` 验证存活进程
- 依赖外部服务或网络的测试应有超时保护，避免测试挂起

#### 运行测试

```bash
# 全部测试（Go 1.21+ 工具链需加 -mod=mod 避免修改 go.mod）
go test -mod=mod ./...

# 仅特定包
go test -mod=mod -v ./src/pkg/job/...

# 运行特定测试
go test -mod=mod -v -run TestStartProcessCmd ./src/pkg/job/...
```

#### 测试覆盖目标
- 每个包含业务逻辑的 `*.go` 文件应有对应的 `*_test.go`
- 平台特定的 `_win.go` / `_unix.go` 文件应有对应的 `_win_test.go` / `_unix_test.go`
- 优先覆盖: 进程管理、配置解析、API 通信、构建流程、升级逻辑

**已有测试覆盖**:

| 包 | 文件 | 覆盖内容 |
|----|------|---------|
| `agentcli` | `cli_test.go` | IsSubcommand、readProperty、preserveSet、cleanup、handleDebug、DebugFileExists、parsePropertiesFile、requiredKeyStatus、intKeyStatus、status 汇总判定 |
| `agentcli` | `i18n_test.go` | msg/msgf、initLang 环境变量优先级、tryReadLang |
| `agentcli` | `status_linux_test.go` | dirStatus、fileStatus、readPid、pidStatus、currentUser |
| `agentcli` | `service_darwin_test.go` | launchdDomain(mode)、serviceTarget、plistDir/Path、writePlist LOGIN/BACKGROUND 模式、readInstallMode、writeInstallType、isProcessAlive、startLogin、stopByMode、currentUID、handleConfigureService |
| `agentcli` | `session_win_test.go` | splitUserDomain、readInstallTypeFile、handleInstall 模式分发和校验、configure-session 收尾摘要本地化 |
| `agentcli` | `diagnose_test.go` | normalizeGateway、buildProxyFunc (含 NoProxy 排除)、loadCertIfExists、detectProxyUsed、tlsVersionName、checkDiskWritable、checkDiskSpace |
| `dockercli` | `dockercli_test.go` | RuntimeBinary、registryFromImage、formatCommand、ImageExists 退出码行为、容器创建时间判断、运行时 socket 选择、`DOCKER_HOST` 优先级、classifyCommandLevel、classifyStreamLevel、looksLikeWarning、NewRunnerWithEvent |
| `job_docker` | `options_test.go` | Docker/Podman CLI 参数构建、network 判断、NeedLocalImageInspect |
| `job` | `docker_runtime_test.go` | 构建容器默认 network/entrypoint/env/mount 参数拼装、Docker CLI 级别到后台 `LogType` 映射、`ERROR/WARN/LOG` 到 `red/yellow/normal` 路由 |
| `api` | `api_test.go` | 构建日志接口路径选择：`/logs`、`/logs/red`、`/logs/yellow` |
| `upgrade` | `upgrade_test.go` | upgradeItems.NoChange |
| `upgrader` | `upgrader_linux_test.go` | checkUpgradeFileChange、replaceAgentFile、modifyScriptPrivateTmp (含权限保留) |
| `upgrader` | `upgrader_win_test.go` | replaceAgentFile (含重试)、replaceMaxRetries |
| `cmd/daemon` | `daemon_win_test.go` | waitForUpgradeFinish (无锁/阻塞/连续调用) |
| `config` | `config_test.go` | GetGateWay、GetAuthHeaderMap、GetPersistedProxyEnvs、SyncPersistedProxyEnvs |
| `config` | `config_linux_test.go` | parseOSRelease、charsToString |
| `httputil` | `devops_test.go` | DevopsResult.IsOk/IsNotOk、AgentResult.IsAgentDelete、HttpResult.IntoDevopsResult/IntoAgentResult |
| `exiterror` | `exiterror_test.go` | AddExitError、GetAndResetExitError、CheckOsIoError、CheckSignalJdkError/WorkerError、CheckTimeoutError、WriteFileWithCheck |
| `envs` | `env_test.go` | GEnvVarsT.Get/SetEnvs/GetAll/Size/RangeDo、FetchEnvAndCheck |
| `util/fileutil` | `fileutil_test.go` | AtomicWriteFile (创建/覆盖/空文件/大文件/临时文件清理) |

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
- **`unsafe.Pointer` 已修复**: `process_exit_group_win.go` 的 `AddProcess()` 已从 `unsafe.Pointer` 方案改为 `windows.OpenProcess()` 通过 PID 获取句柄，不再有 Go 版本布局兼容性问题

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
└── upgrader[.exe]
```

## 调试技巧

1. **Agent日志**: `{workDir}/logs/` 目录下的日志文件
2. **构建错误**: 检查 `build_tmp/{buildId}_{vmSeqId}_build_msg.log`
3. **Docker构建日志**: `docker_build_tmp/logs/{buildId}/{vmSeqId}/`
4. **HTTP通信调试**: 日志中搜索请求URL和响应状态码
5. **升级问题**: 检查 `tmp/` 目录下载的文件和MD5
6. **Windows服务问题**: 使用 `wintask` 包检测启动方式。服务模式下 daemon 按优先级尝试: ① WTS API (`CreateProcessAsUser`) 在已登录用户 Session 中启动 agent ② `LogonUser` + `SetTokenInformation` 使用 `.agent.properties` 中的 `devops.agent.session.user/password` 凭据在控制台 Session 创建进程 ③ 回退为直接启动。推荐使用 `configure_session.ps1` 一键切换 Session 模式（自动迁移 schtasks→service、配置 Auto-logon + LSA Secret 加密存储密码、重启 daemon、提示 reboot），机器开机自动登录后 daemon 即可通过 ① 路径工作
7. **MCP调试**: 设置 `DEVOPS_AGENT_ENABLE_MCP=true`，agent 启动后 MCP Server 在 `127.0.0.1` 监听 Streamable HTTP，端口号持久化到 `.agent.properties` 的 `devops.mcp.server.port`。可在 CodeBuddy/Claude Desktop 中配置 `http://127.0.0.1:{PORT}/mcp` 进行交互式调试

## 环境变量开关

| 环境变量 | 默认 | 说明 |
|---------|------|------|
| `DEVOPS_AGENT_ENABLE_NEW_CONSOLE` | false | Windows 启动进程时使用 newConsole |
| `DEVOPS_AGENT_ENABLE_EXIT_GROUP` | false | 启动杀掉构建进程组的兜底逻辑 |
| `DEVOPS_AGENT_DOCKER_CAP_ADD` | 空 | Docker/Podman 启动容器时追加的 `--cap-add` 参数 |
| `DEVOPS_AGENT_CONTAINER_RUNTIME` | docker | 容器运行时命令，可切换为 `podman` |
| `DEVOPS_AGENT_TIMEOUT_EXIT_TIME` | 空 | 超时次数阈值，达到后Agent进程退出 |
| `DEVOPS_AGENT_CLOSE_FD_INHERIT` | false | Unix 构建进程关闭 fd 继承 (Setpgid + /dev/null + ExtraFiles 清空)，等同 Windows NoInheritHandles |
| `DEVOPS_AGENT_ENABLE_MCP` | false | 随 agent 主进程启动 MCP Server 协程（Streamable HTTP） |

## 注意事项

1. **Go版本锁定**: `go.mod` 中 `go 1.19` 不可随意升级。引入新依赖时必须确认其最低Go版本要求兼容1.19。**禁止**使用 `go mod tidy`（Go 1.21+工具链会自动将go指令升级到1.21），应使用 `GOFLAGS=-mod=mod go build` 来自动更新 go.mod，或通过 gvm 切换到 Go 1.19 后再执行 `go mod tidy`
2. **`unsafe.Pointer`**: `process_exit_group_win.go` 已改用 `windows.OpenProcess()` 方案，不再依赖 `os.Process` 内部字段布局，Go 版本升级无兼容性问题
3. **Telegraf版本锁定**: `go.mod` 中注释说明了 telegraf/gopsutil/docker 版本绑定关系，不可独立升级
4. **环境变量优先级**: API配置变量 > 系统环境变量
5. **构建成功等待**: 构建成功后会 `time.Sleep(8*time.Second)` 等待日志写入
6. **MD5缓存**: Docker Init文件的MD5有缓存机制，避免每次心跳重新计算

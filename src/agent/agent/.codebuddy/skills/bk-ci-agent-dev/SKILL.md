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
d:\projects\bk-ci-agent\src\agent\agent\
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
│   │   ├── job_docker/         # Docker CLI参数解析
│   │   ├── upgrade/            # 升级流程管理
│   │   ├── upgrader/           # upgrader进程逻辑
│   │   ├── installer/          # installer进程逻辑
│   │   ├── imagedebug/         # Docker镜像调试
│   │   ├── pipeline/           # 流水线引擎(实验性)
│   │   ├── envs/               # 环境变量管理
│   │   ├── cron/               # 定时任务
│   │   ├── collector/          # 系统信息采集(Telegraf)
│   │   ├── exiterror/          # 结构化退出错误
│   │   ├── i18n/               # 国际化
│   │   ├── constant/           # 常量(内外版差异)
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
  └─ 主循环 (每5秒):
       api.GetBuild() → 根据返回类型分发:
         ├─ BuildInfo     → job.DoBuild()      // 执行构建
         ├─ UpgradeItem   → upgrade.AgentUpgrade()  // 升级
         ├─ PipelineData  → pipeline.Run()     // 流水线
         └─ ImageDebugInfo→ imagedebug.DoDebug()   // 镜像调试
```

**关键文件**:
- `agent.go` — 主循环、初始化、Ask轮询
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

**重要**: HTTP客户端有超时重试机制，连续超时达阈值会触发Agent退出重启。

### 构建任务执行 (`pkg/job/`)

这是最核心的包，处理二进制构建和Docker构建两种模式。

#### 二进制构建流程:
```
DoBuild(buildInfo)
  → BuildTotalManager.Lock.Lock()     // 全局互斥
  → CheckParallelTaskCount()          // 检查并行数
  → GBuildManager.AddPreInstance()    // 记录预构建
  → Unlock
  → runBuild()                        // goroutine执行
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
  → Docker client 创建
  → 镜像拉取(根据策略)
  → 创建容器:
       挂载: JRE(只读), worker-agent.jar(只读), init.sh(只读), workspace(读写), logs(读写)
       环境变量: project_id, agent_id, secret_key, gateway, build_env=DOCKER
       CapAdd: SYS_PTRACE
       网络: bridge
  → 启动容器
  → ContainerWait() 等待
  → 读取 docker.log / 容器日志
  → dockerBuildFinish() 上报
```

**关键文件**:
- `build.go` — runBuild()、workerBuildFinish()
- `build_docker.go` — runDockerBuild()、Docker相关逻辑
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
// 1. 全局互斥锁 — 构建任务接取
job.BuildTotalManager.Lock.Lock()
defer job.BuildTotalManager.Lock.Unlock()

// 2. 原子计数 — 错误累计
var counter atomic.Int32
counter.Add(1)

// 3. sync.Map — 并发安全的构建实例管理
manager.instances.Store(buildId, instance)

// 4. EventBus — 带缓冲channel的发布订阅
config.GAgentConfig.EventBus.Publish("IpEvent", data)
```

### 新增功能检查清单

- [ ] 是否需要平台特定实现？需要的话创建 `_win.go` / `_unix.go` 等文件
- [ ] 是否需要在 `api/` 新增API调用？遵循现有 `buildUrl` + `request` 模式
- [ ] 是否需要新配置项？在 `config.go` 的 `AgentConfig` 添加字段并在 `LoadAgentConfig()` 解析
- [ ] 是否涉及文件操作？使用 `exiterror.WriteFileWithCheck()` 确保IO错误检测
- [ ] 是否需要定时执行？在 `cron/cron.go` 的 `InitCron()` 注册
- [ ] 是否涉及Docker？确保只在Linux编译(`//go:build linux || darwin`对应)
- [ ] 是否需要国际化？在 `i18n/` 添加翻译

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

### 关键第三方依赖

| 依赖 | 版本 | 用途 | 注意事项 |
|------|------|------|---------|
| `docker/docker` | v24.0.9 | Docker API客户端 | 与telegraf版本绑定 |
| `influxdata/telegraf` | v1.24.4 | 系统指标采集 | 有版本限制，不可随意升级 |
| `shirou/gopsutil/v3` | v3.22.9 | 系统信息获取 | 与telegraf绑定 |
| `pkg/errors` | v0.9.1 | 错误包装 | 项目统一使用 |
| `go-ini/ini` | v1.67.0 | 配置文件解析 | .agent.properties |
| `nicksnyder/go-i18n/v2` | v2.4.1 | 国际化 | |

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
├── .agent.properties               # 主配置文件
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

## 注意事项

1. **`unsafe.Pointer` 使用**: `process_exit_group_win.go` 中通过 unsafe 读取 `os.Process` 内部字段，Go版本升级时需验证内存布局
2. **Telegraf版本锁定**: `go.mod` 中注释说明了 telegraf/gopsutil/docker 版本绑定关系，不可独立升级
3. **环境变量优先级**: API配置变量 > 系统环境变量
4. **构建成功等待**: 构建成功后会 `time.Sleep(8*time.Second)` 等待日志写入
5. **MD5缓存**: Docker Init文件的MD5有缓存机制，避免每次心跳重新计算

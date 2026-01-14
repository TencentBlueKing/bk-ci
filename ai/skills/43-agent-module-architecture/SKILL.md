---
name: 43-agent-module-architecture
description: Agent 构建机模块架构指南（Go 语言），涵盖 Agent 启动流程、心跳机制、任务领取执行、升级更新、与 Dispatch 交互。当用户开发 Agent 功能、修改心跳逻辑、处理任务执行或实现 Agent 升级时使用。
---

# Agent 构建机模块架构指南

> **模块定位**: Agent 是 BK-CI 的构建机核心组件，由 Go 语言编写，负责与后端服务通信、接收构建任务、拉起 Worker 进程执行构建。

## 一、模块概述

### 1.1 核心职责

| 职责 | 说明 |
|------|------|
| **进程管理** | Daemon 守护 Agent 进程，确保持续运行 |
| **任务调度** | 从 Dispatch 服务拉取构建任务并执行 |
| **Worker 管理** | 拉起 Worker（Kotlin JAR）执行实际构建逻辑 |
| **心跳上报** | 定期向后端上报 Agent 状态和环境信息 |
| **自动升级** | 检测并自动升级 Agent、Worker、JDK |
| **数据采集** | 通过 Telegraf 采集构建机指标数据 |
| **Docker 构建** | 支持 Docker 容器化构建（Linux） |

### 1.2 与 Worker 的关系

```
┌─────────────────────────────────────────────────────────────┐
│                    构建机 (Build Machine)                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐     守护      ┌─────────┐                      │
│  │ Daemon  │ ───────────▶ │  Agent  │                      │
│  │  (Go)   │              │  (Go)   │                      │
│  └─────────┘              └────┬────┘                      │
│                                │ 拉起                       │
│                                ▼                            │
│                          ┌─────────┐                        │
│                          │ Worker  │                        │
│                          │(Kotlin) │                        │
│                          └────┬────┘                        │
│                               │ 执行                        │
│                               ▼                             │
│                    ┌──────────────────────┐                │
│                    │ 插件任务 / 脚本任务   │                │
│                    └──────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

- **Agent (Go)**: 负责进程调度、与后端通信、环境管理
- **Worker (Kotlin)**: 负责具体构建任务执行、插件运行、日志上报

## 二、目录结构

```
src/agent/
├── agent/                          # 主 Agent 模块
│   ├── src/
│   │   ├── cmd/                    # 入口程序
│   │   │   ├── agent/main.go       # Agent 主程序入口
│   │   │   ├── daemon/main.go      # Daemon 守护进程入口
│   │   │   ├── installer/main.go   # 安装程序入口
│   │   │   └── upgrader/main.go    # 升级程序入口
│   │   ├── pkg/                    # 核心包
│   │   │   ├── agent/              # Agent 核心逻辑
│   │   │   ├── api/                # API 客户端
│   │   │   ├── collector/          # 数据采集
│   │   │   ├── config/             # 配置管理
│   │   │   ├── cron/               # 定时任务
│   │   │   ├── i18n/               # 国际化
│   │   │   ├── imagedebug/         # Docker 镜像调试
│   │   │   ├── job/                # 构建任务管理
│   │   │   ├── job_docker/         # Docker 构建
│   │   │   ├── pipeline/           # Pipeline 任务
│   │   │   ├── upgrade/            # 升级逻辑
│   │   │   ├── upgrader/           # 升级器实现
│   │   │   └── util/               # 工具函数
│   │   └── third_components/       # 第三方组件管理
│   ├── go.mod
│   ├── Makefile
│   └── README.md
├── agent-slim/                     # 轻量版 Agent
│   └── cmd/slim.go
└── common/                         # 公共工具库
    └── utils/
        ├── fileutil/
        └── slice.go
```

## 三、核心组件详解

### 3.1 Daemon 守护进程

**文件**: `src/cmd/daemon/main.go`

Daemon 负责守护 Agent 进程，确保其持续运行：

```go
// Unix 实现：通过文件锁检测 Agent 是否存活
func watch(isDebug bool) {
    totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
    
    // 首次立即检查
    totalLock.Lock()
    doCheckAndLaunchAgent(isDebug)
    totalLock.Unlock()
    
    // 定时检查（5秒间隔）
    checkTimeTicker := time.NewTicker(agentCheckGap)
    for ; ; totalLock.Unlock() {
        select {
        case <-checkTimeTicker.C:
            if err := totalLock.Lock(); err != nil {
                continue
            }
            doCheckAndLaunchAgent(isDebug)
        }
    }
}

// 检查并拉起 Agent
func doCheckAndLaunchAgent(isDebug bool) {
    agentLock := flock.New(fmt.Sprintf("%s/agent.lock", systemutil.GetRuntimeDir()))
    locked, err := agentLock.TryLock()
    if err == nil && locked {
        // 能获取锁说明 Agent 未运行，需要拉起
        logs.Warn("agent is not available, will launch it")
        process, err := launch(workDir+"/"+config.AgentFileClientLinux, isDebug)
        if err != nil {
            logs.WithError(err).Error("launch agent failed")
        }
    }
}
```

**Windows 实现**: 使用 `github.com/kardianos/service` 库实现 Windows Service

### 3.2 Agent 核心流程

**文件**: `src/pkg/agent/agent.go`

```go
func Run(isDebug bool) {
    // 1. 初始化配置
    config.Init(isDebug)
    third_components.Init()
    
    // 2. 初始化国际化
    i18n.InitAgentI18n()
    
    // 3. 上报启动（重试直到成功）
    _, err := job.AgentStartup()
    if err != nil {
        for {
            _, err = job.AgentStartup()
            if err == nil {
                break
            }
            time.Sleep(5 * time.Second)
        }
    }
    
    // 4. 启动后台任务
    go collector.Collect()      // 数据采集
    go cron.CleanJob()          // 定期清理
    go cron.CleanDebugContainer() // 清理调试容器
    
    // 5. 主循环：Ask 请求
    for {
        doAsk()
        config.LoadAgentIp()
        time.Sleep(5 * time.Second)
    }
}
```

### 3.3 Ask 统一请求模式

Agent 使用 Ask 模式统一处理多种任务：

```go
func doAsk() {
    // 构建 Ask 请求
    enable := genAskEnable()
    heart, upgrad := genHeartInfoAndUpgrade(enable.Upgrade, exiterror)
    
    result, err := api.Ask(&api.AskInfo{
        Enable:  enable,      // 启用的功能
        Heart:   heart,       // 心跳信息
        Upgrade: upgrad,      // 升级信息
    })
    
    // 处理响应
    resp := new(api.AskResp)
    util.ParseJsonToData(result.Data, &resp)
    
    // 执行各类任务
    doAgentJob(enable, resp)
}

func doAgentJob(enable api.AskEnable, resp *api.AskResp) {
    // 心跳响应处理
    if resp.Heart != nil {
        go agentHeartbeat(resp.Heart)
    }
    
    // 构建任务
    hasBuild := (enable.Build != api.NoneBuildType) && (resp.Build != nil)
    if hasBuild {
        go job.DoBuild(resp.Build)
    }
    
    // 升级任务
    if enable.Upgrade && resp.Upgrade != nil {
        go upgrade.AgentUpgrade(resp.Upgrade, hasBuild)
    }
    
    // Pipeline 任务
    if enable.Pipeline && resp.Pipeline != nil {
        go pipeline.RunPipeline(resp.Pipeline)
    }
    
    // Docker 调试
    if enable.DockerDebug && resp.Debug != nil {
        go imagedebug.DoImageDebug(resp.Debug)
    }
}
```

### 3.4 构建任务执行

**文件**: `src/pkg/job/build.go`

```go
// DoBuild 执行构建任务
func DoBuild(buildInfo *api.ThirdPartyBuildInfo) {
    // 获取任务锁
    BuildTotalManager.Lock.Lock()
    
    // 检查并发数
    dockerCanRun, normalCanRun := CheckParallelTaskCount()
    
    if buildInfo.DockerBuildInfo != nil && dockerCanRun {
        // Docker 构建
        GBuildDockerManager.AddBuild(buildInfo.BuildId, &api.ThirdPartyDockerTaskInfo{...})
        BuildTotalManager.Lock.Unlock()
        runDockerBuild(buildInfo)
        return
    }
    
    if normalCanRun {
        // 普通构建
        GBuildManager.AddPreInstance(buildInfo.BuildId)
        BuildTotalManager.Lock.Unlock()
        runBuild(buildInfo)
    }
}

// runBuild 启动 Worker 进程
func runBuild(buildInfo *api.ThirdPartyBuildInfo) error {
    // 检查 worker.jar 是否存在
    agentJarPath := config.BuildAgentJarPath()
    if !fileutil.Exists(agentJarPath) {
        // 尝试自愈
        upgradeWorkerFile := systemutil.GetUpgradeDir() + "/" + config.WorkAgentFile
        if fileutil.Exists(upgradeWorkerFile) {
            fileutil.CopyFile(upgradeWorkerFile, agentJarPath, true)
        }
    }
    
    // 设置环境变量
    goEnv := map[string]string{
        "DEVOPS_AGENT_VERSION":     config.AgentVersion,
        "DEVOPS_WORKER_VERSION":    third_components.Worker.GetVersion(),
        "DEVOPS_PROJECT_ID":        buildInfo.ProjectId,
        "DEVOPS_BUILD_ID":          buildInfo.BuildId,
        "DEVOPS_VM_SEQ_ID":         buildInfo.VmSeqId,
        "DEVOPS_FILE_GATEWAY":      config.GAgentConfig.FileGateway,
        "DEVOPS_GATEWAY":           config.GetGateWay(),
        "BK_CI_LOCALE_LANGUAGE":    config.GAgentConfig.Language,
        "DEVOPS_AGENT_JDK_8_PATH":  third_components.Jdk.Jdk8.GetJavaOrNull(),
        "DEVOPS_AGENT_JDK_17_PATH": third_components.Jdk.Jdk17.GetJavaOrNull(),
    }
    
    // 创建临时目录并启动构建
    tmpDir, _ := systemutil.MkBuildTmpDir()
    doBuild(buildInfo, tmpDir, workDir, goEnv, runUser)
}
```

### 3.5 配置管理

**文件**: `src/pkg/config/config.go`

Agent 配置从 `.agent.properties` 文件加载：

```go
// 配置键定义
const (
    KeyProjectId         = "devops.project.id"
    KeyAgentId           = "devops.agent.id"
    KeySecretKey         = "devops.agent.secret.key"
    KeyDevopsGateway     = "landun.gateway"
    KeyDevopsFileGateway = "landun.fileGateway"
    KeyTaskCount         = "devops.parallel.task.count"
    KeyEnvType           = "landun.env"
    KeySlaveUser         = "devops.slave.user"
    KeyDockerTaskCount   = "devops.docker.parallel.task.count"
    KeyLanguage          = "devops.language"
    // ...
)

// AgentConfig 配置结构
type AgentConfig struct {
    Gateway                 string
    FileGateway             string
    BuildType               string
    ProjectId               string
    AgentId                 string
    SecretKey               string
    ParallelTaskCount       int
    DockerParallelTaskCount int
    EnableDockerBuild       bool
    Language                string
    // ...
}

// AgentEnv 环境信息
type AgentEnv struct {
    OsName           string
    agentIp          string
    HostName         string
    AgentVersion     string
    AgentInstallPath string
    OsVersion        string
    CPUProductInfo   string
    GPUProductInfo   string
}
```

### 3.6 API 客户端

**文件**: `src/pkg/api/api.go`

```go
// 构建 URL
func buildUrl(url string) string {
    return config.GetGateWay() + url
}

// Agent 启动上报
func AgentStartup() (*httputil.DevopsResult, error) {
    url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup")
    startInfo := &ThirdPartyAgentStartInfo{
        HostName:      config.GAgentEnv.HostName,
        HostIp:        config.GAgentEnv.GetAgentIp(),
        DetectOs:      config.GAgentEnv.OsName,
        MasterVersion: config.AgentVersion,
        SlaveVersion:  third_components.Worker.GetVersion(),
    }
    return httputil.NewHttpClient().Post(url).Body(startInfo, false).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

// 构建完成上报
func WorkerBuildFinish(buildInfo *ThirdPartyBuildWithStatus) (*httputil.DevopsResult, error) {
    url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish")
    return httputil.NewHttpClient().Post(url).Body(buildInfo, false).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}

// Ask 统一请求
func Ask(info *AskInfo) (*httputil.AgentResult, error) {
    url := buildUrl("/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask")
    return httputil.NewHttpClient().Post(url).Body(info, bodyEq).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(askRequest.Resp).IntoAgentResult()
}
```

### 3.7 升级机制

**文件**: `src/pkg/upgrade/upgrade.go`

```go
// AgentUpgrade 升级主逻辑
func AgentUpgrade(upgradeItem *api.UpgradeItem, hasBuild bool) {
    upItems := &upgradeItems{
        Agent:          upgradeItem.Agent,
        Worker:         upgradeItem.Worker,
        Jdk:            upgradeItem.Jdk,
        DockerInitFile: upgradeItem.DockerInitFile,
    }
    
    if upItems.NoChange() {
        return
    }
    
    // 有构建任务时跳过升级
    if hasBuild {
        return
    }
    
    // 获取任务锁，确保无任务运行
    if !job.BuildTotalManager.Lock.TryLock() {
        return
    }
    defer job.BuildTotalManager.Lock.Unlock()
    
    if job.CheckRunningJob() {
        return
    }
    
    // 下载升级文件
    downloadUpgradeFiles(upItems)
    
    // 执行升级
    DoUpgradeOperation(upItems)
}
```

### 3.8 数据采集

**文件**: `src/pkg/collector/collector.go`

使用 Telegraf 进行数据采集：

```go
func Collect() {
    if config.GAgentConfig.CollectorOn == false {
        logs.Info("agent collector off")
        return
    }
    
    for {
        ctx, cancel := context.WithCancel(context.Background())
        go func() {
            // 监听 IP 变化事件
            ipData := <-ipChan.DChan
            cancel()
        }()
        doAgentCollect(ctx)
    }
}

func doAgentCollect(ctx context.Context) {
    // 生成 Telegraf 配置
    configContent, _ := genTelegrafConfig()
    
    // 初始化 Telegraf Agent
    tAgent, _ := getTelegrafAgent(configContent.Bytes(), logFile)
    
    // 运行采集
    for {
        tAgent.Run(ctx)
        time.Sleep(telegrafRelaunchTime)
    }
}
```

## 四、数据类型定义

### 4.1 构建信息

**文件**: `src/pkg/api/type.go`

```go
// 构建任务类型
type BuildJobType string

const (
    AllBuildType    BuildJobType = "ALL"
    DockerBuildType BuildJobType = "DOCKER"
    BinaryBuildType BuildJobType = "BINARY"
    NoneBuildType   BuildJobType = "NONE"
)

// 第三方构建信息
type ThirdPartyBuildInfo struct {
    ProjectId       string                     `json:"projectId"`
    BuildId         string                     `json:"buildId"`
    VmSeqId         string                     `json:"vmSeqId"`
    Workspace       string                     `json:"workspace"`
    PipelineId      string                     `json:"pipelineId"`
    DockerBuildInfo *ThirdPartyDockerBuildInfo `json:"dockerBuildInfo"`
    ExecuteCount    *int                       `json:"executeCount"`
    ContainerHashId string                     `json:"containerHashId"`
}

// Docker 构建信息
type ThirdPartyDockerBuildInfo struct {
    AgentId         string        `json:"agentId"`
    SecretKey       string        `json:"secretKey"`
    Image           string        `json:"image"`
    Credential      Credential    `json:"credential"`
    Options         DockerOptions `json:"options"`
    ImagePullPolicy string        `json:"imagePullPolicy"`
}
```

### 4.2 心跳信息

```go
// Agent 心跳信息
type AgentHeartbeatInfo struct {
    MasterVersion           string                     `json:"masterVersion"`
    SlaveVersion            string                     `json:"slaveVersion"`
    HostName                string                     `json:"hostName"`
    AgentIp                 string                     `json:"agentIp"`
    ParallelTaskCount       int                        `json:"parallelTaskCount"`
    AgentInstallPath        string                     `json:"agentInstallPath"`
    StartedUser             string                     `json:"startedUser"`
    TaskList                []ThirdPartyTaskInfo       `json:"taskList"`
    DockerParallelTaskCount int                        `json:"dockerParallelTaskCount"`
    DockerTaskList          []ThirdPartyDockerTaskInfo `json:"dockerTaskList"`
}

// 心跳响应
type AgentHeartbeatResponse struct {
    MasterVersion           string            `json:"masterVersion"`
    SlaveVersion            string            `json:"slaveVersion"`
    AgentStatus             string            `json:"agentStatus"`
    ParallelTaskCount       int               `json:"parallelTaskCount"`
    Envs                    map[string]string `json:"envs"`
    Gateway                 string            `json:"gateway"`
    FileGateway             string            `json:"fileGateway"`
    DockerParallelTaskCount int               `json:"dockerParallelTaskCount"`
    Language                string            `json:"language"`
}
```

## 五、跨平台支持

### 5.1 平台特定代码

Agent 通过 Go 的构建标签支持多平台：

```
src/pkg/config/
├── config.go           # 通用配置
├── config_darwin.go    # macOS 特定
├── config_linux.go     # Linux 特定
└── config_win.go       # Windows 特定

src/pkg/upgrader/
├── upgrader_darwin.go  # macOS 升级器
├── upgrader_unix.go    # Unix 升级器
└── upgrader_win.go     # Windows 升级器
```

### 5.2 构建命令

```bash
# Linux
make clean build_linux

# macOS
make clean build_macos

# Windows
build_windows.bat
```

生成的二进制文件：
- `devopsDaemon_linux` / `devopsDaemon_macos` / `devopsDaemon.exe`
- `devopsAgent_linux` / `devopsAgent_macos` / `devopsAgent.exe`
- `upgrader_linux` / `upgrader_macos` / `upgrader.exe`

## 六、与后端服务交互

### 6.1 API 端点

| 服务 | 端点 | 用途 |
|------|------|------|
| Environment | `/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup` | Agent 启动上报 |
| Dispatch | `/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask` | 统一 Ask 请求 |
| Dispatch | `/ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish` | 构建完成上报 |
| Environment | `/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/pipelines` | Pipeline 任务 |
| Environment | `/ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/files/download` | 下载升级文件 |

### 6.2 认证头

```go
func (a *AgentConfig) GetAuthHeaderMap() map[string]string {
    return map[string]string{
        "X-DEVOPS-BUILD-TYPE": a.BuildType,
        "X-DEVOPS-PROJECT-ID": a.ProjectId,
        "X-DEVOPS-AGENT-ID":   a.AgentId,
        "X-DEVOPS-AGENT-SECRET-KEY": a.SecretKey,
    }
}
```

## 七、开发规范

### 7.1 错误处理

```go
// 标准错误检查
if err != nil {
    logs.WithError(err).Error("operation failed")
    return errors.Wrap(err, "context message")
}

// Panic 恢复
defer func() {
    if err := recover(); err != nil {
        logs.Error("panic: ", err)
    }
}()
```

### 7.2 日志规范

```go
// 日志级别
logs.Debug("debug message")
logs.Info("info message")
logs.Infof("formatted: %s", value)
logs.Warn("warning message")
logs.Error("error message")
logs.WithError(err).Error("error with context")
```

### 7.3 并发模式

```go
// 启动 goroutine
go collector.Collect()
go cron.CleanJob()

// 使用锁保护共享资源
BuildTotalManager.Lock.Lock()
defer BuildTotalManager.Lock.Unlock()

// 使用文件锁进行进程间同步
agentLock := flock.New(fmt.Sprintf("%s/agent.lock", runtimeDir))
locked, err := agentLock.TryLock()
```

### 7.4 新增功能开发

1. **新增 API 调用**：在 `src/pkg/api/api.go` 添加函数
2. **新增数据类型**：在 `src/pkg/api/type.go` 定义结构体
3. **新增配置项**：在 `src/pkg/config/config.go` 添加常量和字段
4. **新增后台任务**：在 `doAgentJob()` 中添加处理逻辑

## 八、控制脚本

```bash
# Linux 示例
scripts/linux/install.sh    # 安装
scripts/linux/start.sh      # 启动
scripts/linux/stop.sh       # 停止
scripts/linux/uninstall.sh  # 卸载
```

## 九、相关模块

| 模块 | 关系 | 说明 |
|------|------|------|
| Worker | 下游 | Agent 拉起 Worker 执行构建 |
| Environment | 上游 | Agent 状态管理、心跳上报 |
| Dispatch | 上游 | 构建任务分发 |
| Log | 下游 | 构建日志上报（通过 Worker） |

---
name: go-agent-development
description: Go Agent 开发指南，涵盖 Agent 架构设计、心跳机制、任务执行、日志上报、升级流程、与 Dispatch 模块交互。当用户开发构建机 Agent、实现任务执行逻辑、处理 Agent 通信或进行 Go 语言开发时使用。
core_files:
  - "src/agent/agent/src/pkg/"
  - "src/agent/agent-slim/"
related_skills:
  - dispatch-module-architecture
  - agent-module-architecture
token_estimate: 2500
---

# Go Agent 开发

## Quick Reference

```
Go 版本：1.19+
进程组成：DevopsDaemon（守护进程） + DevopsAgent（主进程）
核心包：api/（API 调用）| config/（配置）| job/（任务）| pipeline/（流水线）
日志：logs.Debug/Info/Error/WithError
```

### 最简示例

```go
// API 调用
func AgentStartup() (*httputil.DevopsResult, error) {
    url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup")
    startInfo := &ThirdPartyAgentStartInfo{
        HostName:      systemutil.GetHostName(),
        HostIp:        systemutil.GetAgentIp(),
        DetectOs:      systemutil.GetOsName(),
        MasterVersion: config.AgentVersion,
    }
    return httputil.NewHttpClient().Post(url).Body(startInfo, false).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}
```

## When to Use

- 开发构建机 Agent
- 实现任务执行逻辑
- 处理 Agent 与后端通信
- 编写 Go 工具函数

## When NOT to Use

- 后端 Kotlin/Java 开发 → 使用 `backend-microservice-development`
- 调度模块业务逻辑 → 使用 `dispatch-module-architecture`

---

## 项目结构

```
src/agent/
├── agent/                 # 主代理
│   └── src/pkg/
│       ├── api/           # API 调用
│       ├── config/        # 配置管理
│       ├── collector/     # 数据采集
│       ├── job/           # 任务执行
│       └── util/          # 工具函数
├── agent-slim/            # 轻量版代理
└── common/                # 通用库
```

## 命名规范

```go
// 包命名：小写单词，不使用下划线
package api
package config

// 结构体：PascalCase
type ThirdPartyAgentStartInfo struct {
    HostName      string `json:"hostname"`
    HostIp        string `json:"hostIp"`
}

// 常量：驼峰命名
const (
    KeyProjectId     = "devops.project.id"
    KeyAgentId       = "devops.agent.id"
)

// 枚举
type BuildJobType string
const (
    AllBuildType    BuildJobType = "ALL"
    DockerBuildType BuildJobType = "DOCKER"
)
```

## 配置管理

```go
type AgentConfig struct {
    Gateway           string
    ProjectId         string
    AgentId           string
    SecretKey         string
    ParallelTaskCount int
}

var GAgentConfig *AgentConfig

func GetGateWay() string {
    return GAgentConfig.Gateway
}
```

## 错误处理

```go
// 标准错误检查
if err != nil {
    logs.WithError(err).Error("agent startup failed")
    return
}

// 重试模式
for {
    _, err = job.AgentStartup()
    if err == nil {
        break
    }
    logs.WithError(err).Error("retry startup")
    time.Sleep(5 * time.Second)
}

// Panic 恢复
defer func() {
    if err := recover(); err != nil {
        logs.Error("panic recovered: ", err)
    }
}()
```

## 日志规范

```go
logs.Debug("debug message")
logs.Info("info message")
logs.Infof("formatted: %s", data)
logs.Error("error message")
logs.WithError(err).Error("with error context")
```

## 并发模式

```go
// 启动 goroutine
go collector.Collect()
go cron.CleanJob()

// 使用 defer 清理
defer config.EBus.Unsubscribe(config.IpEvent, eBusId)

// Channel 通信
done := make(chan bool)
go func() {
    // do work
    done <- true
}()
<-done
```

## HTTP 客户端

```go
httputil.NewHttpClient().
    Post(url).
    Body(data, false).
    SetHeaders(headers).
    Execute(nil).
    IntoDevopsResult()
```

---

## Checklist

开发 Agent 功能前确认：
- [ ] 使用标准错误处理模式
- [ ] 添加适当的日志记录
- [ ] goroutine 有 panic 恢复
- [ ] 网络调用有重试机制
- [ ] 资源使用 defer 清理

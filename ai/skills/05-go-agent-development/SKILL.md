---
name: 05-go-agent-development
description: Go Agent 开发指南，涵盖 Agent 架构设计、心跳机制、任务执行、日志上报、升级流程、与 Dispatch 模块交互。当用户开发构建机 Agent、实现任务执行逻辑、处理 Agent 通信或进行 Go 语言开发时使用。
---

# Skill 05: Go Agent 开发

## 概述
BK-CI 的构建代理（Agent）使用 Go 语言编写，负责与后端服务通信、执行构建任务。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Go | 1.19+ | 编程语言 |
| go.mod | - | 依赖管理 |
| agentcommon/logs | - | 日志框架 |

## 项目结构

```
src/agent/
├── agent/                 # 主代理
│   └── src/pkg/
│       ├── api/           # API 调用
│       ├── config/        # 配置管理
│       ├── collector/     # 数据采集
│       ├── job/           # 任务执行
│       ├── pipeline/      # 流水线处理
│       └── util/          # 工具函数
├── agent-slim/            # 轻量版代理
└── common/                # 通用库
```

## 命名规范

### 包命名

- 小写单词，不使用下划线
- 示例：`agent`, `api`, `config`, `collector`, `job`, `pipeline`, `util`

### 结构体命名

使用 PascalCase：

```go
type ThirdPartyAgentStartInfo struct {
    HostName      string `json:"hostname"`
    HostIp        string `json:"hostIp"`
    DetectOs      string `json:"detectOS"`
    MasterVersion string `json:"masterVersion"`
    SlaveVersion  string `json:"version"`
}

type ThirdPartyBuildInfo struct {
    ProjectId       string   `json:"projectId"`
    BuildId         string   `json:"buildId"`
    ToDelTmpFiles   []string `json:"-"` // 不序列化字段
}
```

### 常量定义

```go
const (
    KeyProjectId     = "devops.project.id"
    KeyAgentId       = "devops.agent.id"
    KeySecretKey     = "devops.agent.secret.key"
    KeyDevopsGateway = "landun.gateway"
)
```

### 枚举类型

```go
type BuildJobType string

const (
    AllBuildType    BuildJobType = "ALL"
    DockerBuildType BuildJobType = "DOCKER"
    BinaryBuildType BuildJobType = "BINARY"
)
```

## 配置管理

```go
// config/config.go
type AgentConfig struct {
    Gateway           string
    ProjectId         string
    AgentId           string
    SecretKey         string
    ParallelTaskCount int
}

var GAgentConfig *AgentConfig

func Init() {
    GAgentConfig = &AgentConfig{
        Gateway:   getConfigValue(KeyDevopsGateway),
        ProjectId: getConfigValue(KeyProjectId),
        AgentId:   getConfigValue(KeyAgentId),
        SecretKey: getConfigValue(KeySecretKey),
    }
}

func GetGateWay() string {
    return GAgentConfig.Gateway
}
```

## API 调用模式

```go
// api/api.go
func buildUrl(url string) string {
    return config.GetGateWay() + url
}

func AgentStartup() (*httputil.DevopsResult, error) {
    url := buildUrl("/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup")
    startInfo := &ThirdPartyAgentStartInfo{
        HostName:      systemutil.GetHostName(),
        HostIp:        systemutil.GetAgentIp(),
        DetectOs:      systemutil.GetOsName(),
        MasterVersion: config.AgentVersion,
        SlaveVersion:  config.WorkerVersion,
    }
    return httputil.NewHttpClient().Post(url).Body(startInfo, false).
        SetHeaders(config.GAgentConfig.GetAuthHeaderMap()).Execute(nil).IntoDevopsResult()
}
```

## 错误处理模式

### 标准错误检查

```go
if err != nil {
    logs.WithError(err).Error("init third_components error")
    systemutil.ExitProcess(1)
}
```

### 重试模式

```go
_, err := job.AgentStartup()
if err != nil {
    logs.WithError(err).Error("agent startup failed")
    for {
        _, err = job.AgentStartup()
        if err == nil {
            break
        }
        logs.WithError(err).Error("agent startup failed")
        time.Sleep(5 * time.Second)
    }
}
```

### Panic 恢复

```go
defer func() {
    if err := recover(); err != nil {
        logs.Error("agent collect panic: ", err)
    }
}()
```

## 日志记录规范

```go
// 日志级别使用
logs.Debug("do Collect")
logs.Info("agent collector off")
logs.Infof("collect ip change data: %s", ipData.Data)
logs.Error("agent collect panic: ", err)
logs.WithError(err).Error("init third_components error")
```

## 并发模式

### 启动 goroutine

```go
go collector.Collect()
go cron.CleanJob()
```

### 使用 defer 清理资源

```go
defer config.EBus.Unsubscribe(config.IpEvent, eBusId)
```

### Channel 通信

```go
// 创建 channel
done := make(chan bool)

// 发送数据
done <- true

// 接收数据
<-done
```

## 测试规范

### 测试文件命名

`*_test.go`

### 测试示例

```go
// build_test.go
package job

import (
    "testing"
)

func TestBuildJob(t *testing.T) {
    // 测试逻辑
}

func TestParseJobType(t *testing.T) {
    tests := []struct {
        input    string
        expected BuildJobType
    }{
        {"ALL", AllBuildType},
        {"DOCKER", DockerBuildType},
        {"BINARY", BinaryBuildType},
    }
    
    for _, tt := range tests {
        result := ParseJobType(tt.input)
        if result != tt.expected {
            t.Errorf("ParseJobType(%s) = %v, want %v", tt.input, result, tt.expected)
        }
    }
}
```

## HTTP 客户端封装

```go
type HttpClient struct {
    client  *http.Client
    request *http.Request
}

func NewHttpClient() *HttpClient {
    return &HttpClient{
        client: &http.Client{
            Timeout: 30 * time.Second,
        },
    }
}

func (c *HttpClient) Get(url string) *HttpClient {
    req, _ := http.NewRequest("GET", url, nil)
    c.request = req
    return c
}

func (c *HttpClient) Post(url string) *HttpClient {
    req, _ := http.NewRequest("POST", url, nil)
    c.request = req
    return c
}

func (c *HttpClient) SetHeaders(headers map[string]string) *HttpClient {
    for k, v := range headers {
        c.request.Header.Set(k, v)
    }
    return c
}

func (c *HttpClient) Execute(body interface{}) *HttpResponse {
    resp, err := c.client.Do(c.request)
    // 处理响应
    return &HttpResponse{resp: resp, err: err}
}
```

## 系统工具函数

```go
// util/systemutil/systemutil.go
func GetHostName() string {
    hostname, _ := os.Hostname()
    return hostname
}

func GetAgentIp() string {
    addrs, _ := net.InterfaceAddrs()
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
            if ipnet.IP.To4() != nil {
                return ipnet.IP.String()
            }
        }
    }
    return ""
}

func GetOsName() string {
    return runtime.GOOS
}

func ExitProcess(code int) {
    os.Exit(code)
}
```

## 进程管理

Agent 由两个进程组成：
- **DevopsDaemon**：守护进程，负责启动和监控 Agent
- **DevopsAgent**：主进程，负责与服务端通信和执行任务

```go
// 启动 Worker 进程
func StartWorker(buildInfo *ThirdPartyBuildInfo) error {
    cmd := exec.Command(workerPath, buildInfo.BuildId)
    cmd.Dir = workDir
    cmd.Env = os.Environ()
    return cmd.Start()
}
```

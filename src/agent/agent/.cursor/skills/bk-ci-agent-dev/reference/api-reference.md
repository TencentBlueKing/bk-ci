# API 接口参考

## API 基础设施

### URL 构建

```go
// api/api.go — buildUrl()
// 格式: {gateway}/{path}
// 示例: https://devops.example.com/ms/environment/api/buildAgent/agent/thirdPartyAgent/startup
```

### 鉴权 Header

所有请求携带:
```
X-DEVOPS-PROJECT-ID: {projectId}
X-DEVOPS-AGENT-ID: {agentId}
X-DEVOPS-AGENT-SECRET-KEY: {secretKey}
```

通过 `config.GAgentConfig.GetAuthHeaderMap()` 获取。

### 响应格式

```go
type DevopsResult struct {
    Status int         `json:"status"`   // 0=成功
    Data   interface{} `json:"data"`
}

// ErrorEnum 定义的状态码:
// 0    — Ok
// 2127 — 任务已经被认领
// 2128 — Agent环境升级
```

### HTTP 客户端

使用 `httputil.NewHttpClient()` 创建，支持:
- `.Get(url)` / `.Post(url)` / `.Put(url)` / `.Delete(url)`
- `.SetHeaders(map)` — 设置请求头
- `.Body(reader)` — 设置请求体
- `.SetTimeoutRate(seconds)` — 设置超时
- `.Execute()` — 执行请求

超时处理: 连续超时会递增计数器，达到阈值触发Agent退出重启。

---

## 核心 API 列表

### 1. GetBuild — 轮询获取任务

**路径**: `GET /ms/dispatch/api/buildAgent/agent/thirdPartyAgent/startup`

**返回类型枚举** (`AgentActionType`):
| 值 | 含义 | 处理函数 |
|---|------|---------|
| `BuildInfo` | 构建任务 | `job.DoBuild()` |
| `UpgradeItem` | 升级指令 | `upgrade.AgentUpgrade()` |
| `PipelineData` | 流水线数据 | `pipeline.Run()` |
| `ImageDebugInfo` | 镜像调试 | `imagedebug.DoDebug()` |

### 2. GetAgentStatus — 心跳上报

**路径**: `POST /ms/environment/api/buildAgent/agent/thirdPartyAgent/status`

**请求体** (`AgentHeartbeatInfo`):
```go
type AgentHeartbeatInfo struct {
    MasterVersion     string
    SlaveVersion      string
    HostName          string
    AgentIp           string
    ParallelTaskCount int
    AgentInstallPath  string
    StartedUser       string
    TaskList          []ThirdPartyTaskInfo  // 运行中的构建
    Props             AgentPropsInfo        // 系统信息
    DockerParallelTaskCount int
    DockerTaskList    []ThirdPartyDockerTaskInfo
    AllTaskCount      int
    EnablePipeline    bool
    DockerInitFileInfo *DockerFileMd5
    ErrorExitData     *exiterror.ExitErrorType
}
```

### 3. WorkerBuildFinish — 上报构建完成

**路径**: `POST /ms/dispatch/api/buildAgent/agent/thirdPartyAgent/workerBuildFinish`

**请求体** (`ThirdPartyBuildWithStatus`):
```go
type ThirdPartyBuildWithStatus struct {
    ProjectId string
    BuildId   string
    VmSeqId   string
    Success   bool
    Message   string
    ErrorCode int
}
```

### 4. DownloadUpgradeFile — 下载升级文件

**路径**: `GET /ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/{fileName}`

**查询参数**: `eTag={localFileMd5}` — 服务端比对，相同返回304

**响应头**: `X-Checksum-Md5` — 下载完成后验证

### 5. FinishUpgrade — 上报升级结果

**路径**: `PUT /ms/environment/api/buildAgent/agent/thirdPartyAgent/upgradeResult`

**请求体**:
```json
{
    "success": true,
    "agentVersion": "v1.x.x",
    "workerVersion": "v1.x.x",
    "jdkVersion": ["8.x", "17.x"]
}
```

### 6. DownloadDockerInitFile — 下载Docker初始化脚本

**路径**: `GET /ms/dispatch/api/buildAgent/agent/thirdPartyAgent/docker_init.sh`

### 7. UploadPipelineData — 上报流水线执行数据

**路径**: `POST /ms/environment/api/buildAgent/agent/thirdPartyAgent/pipelines/data`

---

## 数据类型详解

### BuildInfo — 构建任务信息

```go
type ThirdPartyBuildInfo struct {
    ProjectId         string
    BuildId           string
    VmSeqId           string
    Workspace         string
    PipelineId        string
    DockerBuildInfo   *ThirdPartyDockerBuildInfo  // 非nil则为Docker构建
    ExtraProjectId    string
}
```

### DockerBuildInfo — Docker构建信息

```go
type ThirdPartyDockerBuildInfo struct {
    AgentId         string
    SecretKey       string
    Image           string          // 镜像名
    Credential      *Credential     // 仓库认证
    Options         *DockerOptions  // Docker CLI参数
    ImagePullPolicy string          // Always/IfNotPresent/Never
    Envs            map[string]string
}
```

### UpgradeItem — 升级信息

```go
type UpgradeItem struct {
    Agent          bool
    Worker         bool
    Jdk            bool
    DockerInitFile bool
}
```

### AgentPropsInfo — 系统属性

```go
type AgentPropsInfo struct {
    Arch              string   // runtime.GOARCH
    Ncpus             int      // CPU核数
    HostIp            string
    HostName          string
    DockerInitFileMd5 string
    JdkVersion        []string // JDK版本列表
    GpuInfo           string   // GPU型号
}
```

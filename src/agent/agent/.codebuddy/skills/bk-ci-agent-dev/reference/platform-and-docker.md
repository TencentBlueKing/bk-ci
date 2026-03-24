# 平台适配与 Docker 开发参考

## 平台适配模式

### 添加新的平台特定功能

当需要在不同平台有不同实现时，遵循以下模式：

#### 方式一：文件后缀（推荐）

创建同名但不同后缀的文件，Go编译器自动选择：

```
feature.go          # 通用代码或Unix实现 (需要build tag)
feature_win.go      # Windows实现
feature_linux.go    # Linux专用
feature_darwin.go   # macOS专用
```

#### 方式二：Build Tags

在文件开头添加构建约束：

```go
//go:build linux || darwin

package mypackage
```

#### 项目中的实际示例

**进程启动** (`job/`):
- `do_build.go` — Unix: 写Shell脚本 → bash执行
- `do_build_win.go` — Windows: 直接java -jar

**环境变量** (`envs/`):
- `env_polling_nowin.go` — 非Windows: 简单返回 `os.Environ()`
- `env_polling_win.go` — Windows: 每3秒轮询注册表，合并系统+用户PATH

**文件重命名** (`internal/third_party/dep/fs/`):
- `rename.go` — Unix: `os.Rename` + 跨设备fallback(copy+delete)
- `rename_windows.go` — Windows: 同理但使用 `moveFileEx` API fallback

### Windows 特殊注意事项

1. **进程组管理**: 使用 Windows Job Object (`process_exit_group_win.go`)
   ```go
   // 创建Job Object → 设置 JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
   // → AssignProcessToJobObject → 关闭Job时自动杀所有子进程
   ```

2. **unsafe.Pointer 读取 os.Process**: 直接转换内存布局获取Handle
   ```go
   windows.Handle((*process)(unsafe.Pointer(p)).Handle)
   ```
   Go版本升级时需验证 `os.Process` 结构体布局未变。

3. **Windows服务检测**: `wintask/wintask.go` 检测三种启动方式
   - `SERVICE`: Windows服务 (`devops_agent_{agentId}`)
   - `TASK`: 计划任务
   - `MANUAL`: 手动启动

4. **注册表环境变量**: Windows不自动继承环境变量变更，需主动轮询注册表
   - 系统变量: `HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment`
   - 用户变量: `HKCU\Environment`
   - PATH特殊处理: 系统PATH + ";" + 用户PATH

---

## Docker 开发详解

### Docker 容器创建参数

#### 挂载点

```go
mounts := []mount.Mount{
    {Type: mount.TypeBind, Source: jdk17Path, Target: "/usr/local/jre", ReadOnly: true},
    {Type: mount.TypeBind, Source: jdk8Path, Target: "/usr/local/jre8", ReadOnly: true},   // 仅在有JDK17时
    {Type: mount.TypeBind, Source: workerJarPath, Target: "/data/worker-agent.jar", ReadOnly: true},
    {Type: mount.TypeBind, Source: initShPath, Target: "/data/init.sh", ReadOnly: true},
    {Type: mount.TypeBind, Source: dataDir, Target: DockerDataDir},                         // 读写
    {Type: mount.TypeBind, Source: logDir, Target: "/data/logs"},                            // 读写
}
```

#### 环境变量注入

```go
envs := []string{
    "devops_project_id=" + config.GAgentConfig.ProjectId,
    "devops_agent_id=" + config.GAgentConfig.AgentId,
    "devops_agent_secret_key=" + config.GAgentConfig.SecretKey,
    "devops_gateway=" + config.GAgentConfig.Gateway,
    "devops_file_gateway=" + config.GAgentConfig.FileGateway,
    "agent_build_env=DOCKER",
    "TERM=xterm-256color",
    "build_id=" + buildInfo.BuildId,
    "vm_seq_id=" + buildInfo.VmSeqId,
    // + API配置的环境变量 (GApiEnvVars)
    // + DockerBuildInfo.Envs 用户自定义变量
}
```

#### CapAdd 配置

默认添加 `SYS_PTRACE`，可通过环境变量 `DEVOPS_AGENT_DOCKER_CAP_ADD` 自定义：
```go
// 格式: 逗号分隔
// 例: DEVOPS_AGENT_DOCKER_CAP_ADD=SYS_PTRACE,NET_ADMIN
```

#### Docker Options 解析 (`job_docker/`)

`ParseDockerOptions()` 解析 Docker CLI 风格的参数字符串，支持:
- `-v /host:/container` — 挂载
- `-e KEY=VALUE` — 环境变量
- `--network` — 网络模式
- `--memory` / `--cpus` — 资源限制
- 等完整的 docker run 参数

#### 镜像拉取策略

```go
switch imagePullPolicy {
case "Always":    // 总是拉取
case "IfNotPresent": // 本地不存在才拉取
case "Never":     // 不拉取
default:
    if strings.HasSuffix(image, ":latest") {
        // latest 标签总是拉取
    } else {
        // 其他标签 IfNotPresent
    }
}
```

#### Docker 认证

```go
func GenerateDockerAuth(credential *api.Credential) string {
    authConfig := registry.AuthConfig{
        Username: credential.User,
        Password: credential.Password,
    }
    // Base64 编码后传递给 ImagePull
}
```

### 调试容器 (`imagedebug/`)

独立的Docker镜像调试功能:
- **管理器**: `DebugManager` 管理调试容器生命周期
- **WebSocket Console**: 提供交互式终端连接
- **端口管理**: 在配置范围内分配调试端口
- **自动清理**: 每4小时清理运行超过24小时的调试容器(前缀 `debug-b-`)

---

## 第三方组件管理 (`third_components/`)

### Worker JAR

```go
// worker.go — Worker版本检测与管理
func PrepareWorkerFile(buildInfo) error
    → 检查 worker-agent.jar 是否存在
    → 获取当前版本(java -jar worker-agent.jar version)
```

### JDK 管理

```go
// jdk.go — JDK路径获取与版本检测
func GetJava() string       // 获取默认Java路径(JDK8或JDK17)
func GetJdk17Java() string  // 获取JDK17 Java路径
func GetJdkVersion() []string // 获取所有JDK版本
```

JDK查找优先级:
1. 配置文件指定路径 (`devops.agent.jdk.dir.path`)
2. 工作目录下 `jdk/bin/java`
3. 系统 PATH 中的 `java`

---

## 系统信息采集 (`collector/`)

使用 Telegraf 采集系统指标:
- CPU 使用率
- 内存使用量
- 磁盘使用量和IO
- 网络流量

采集结果通过心跳上报给服务端。

**注意**: Telegraf版本锁定在 v1.24.4，相关依赖(gopsutil v3.22.9, docker v24.0.9)不可独立升级。

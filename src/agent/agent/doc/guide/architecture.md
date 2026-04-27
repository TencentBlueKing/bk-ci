# Agent架构与模块说明

本文档介绍 BK-CI Agent 的整体架构、进程模型、模块职责和内部工作流程。

## 进程模型

Agent 由三层进程组成：

```
┌─────────────────────────────────────────────────┐
                  操作系统服务层                  
        systemd / launchd / Windows SCM          
└──────────────────────┬──────────────────────────┘
                       │ 管理
                       ▼
┌─────────────────────────────────────────────────┐
                devopsDaemon (守护进程)              
        职责: 监控 devopsAgent, 异常退出时自动重启          
└──────────────────────┬──────────────────────────┘
                       │ 启动/监控
                       ▼
┌─────────────────────────────────────────────────┐
                devopsAgent (主进程)                  
        职责: 心跳、构建调度、升级、配置管理                  
└──────────┬─────────────────────────┬────────────┘
           │ 启动                    │ 启动
           ▼                         ▼
┌──────────────────┐     ┌──────────────────────┐
  worker-agent.jar               Docker 容器          
  (Java 构建进程)                   (可选)               
└──────────────────┘     └─┬────────────────────┘
                           │ 启动
                           ▼
                         ┌──────────────────┐ 
                            worker-agent.jar   
                            (Java 构建进程)      
                         └──────────────────┘                   

```

| 进程 | 二进制文件 | 说明 |
|------|----------|------|
| 守护进程 | `devopsDaemon` | 监控 Agent 主进程，异常退出时自动重启。通过 PID 文件 (`runtime/daemon.pid`) 管理 |
| 主进程 | `devopsAgent` | Agent 核心，负责与服务端通信、接收和分派任务。通过 PID 文件 (`runtime/agent.pid`) 管理 |
| 构建进程 | `worker-agent.jar` | Java 进程，执行具体的流水线构建逻辑。每个构建任务启动独立进程 |

`devopsAgent` 是单一二进制，具有两种运行模式：
- **CLI 模式** — 带子命令参数运行（如 `devopsAgent install`），执行完即退出
- **Agent模式** — 不带参数运行，进入主循环持续工作

## 主循环流程

Agent 启动后进入一个每 5 秒执行一次的轮询循环：

```
启动
 │
 ├── 加载配置 (.agent.properties)
 ├── 初始化证书、环境变量
 ├── 启动环境变量轮询 (Windows: 3s 刷新注册表)
 │
 ├── AgentStartup 注册 (重试直到成功)
 │
 ├── 启动后台任务:
 │   ├── 指标采集 (Collector)
 │   ├── 定期清理 (Cron)
 │   └── MCP Server (可选)
 │
 └── 主循环 (每 5 秒):
     │
     ├── Ask 请求 → 服务端
     │   ├── 上报心跳 (版本、IP、主机名、运行中的任务)
     │   ├── 上报升级信息 (Worker/JDK 版本、MD5)
     │   └── 请求新任务
     │
     ├── 处理响应:
     │   ├── Heartbeat → 同步配置、环境变量、代理
     │   ├── Build → 启动构建 (普通/Docker)
     │   ├── Upgrade → 下载更新并替换二进制
     │   ├── Pipeline → 执行流水线任务
     │   └── DockerDebug → 容器镜像调试
     │
     └── 刷新 Agent IP → sleep 5s → 循环
```

## 模块清单

Agent 代码以 Go 包的形式组织在 `src/pkg/` 下：

### 核心模块

| 模块 | 包路径 | 职责 | 确认正常的方法 |
|------|--------|------|-------------|
| agent | `pkg/agent` | 主循环、Ask 轮询、心跳处理、任务分派 | `status` 查看 Agent PID |
| config | `pkg/config` | 加载/持久化 `.agent.properties`，管理 TLS 证书 | 检查 `.agent.properties` 文件完整性 |
| api | `pkg/api` | 与 BK-CI 服务端的 HTTP 通信客户端 | `status` 健康检查的 HTTP 连通性测试 |

### 构建模块

| 模块 | 包路径 | 职责 | 确认正常的方法 |
|------|--------|------|-------------|
| job | `pkg/job` | 构建任务调度、Worker 进程生命周期管理、并发控制 | `status` 查看 `worker-agent.jar` 是否存在 |
| job_docker | `pkg/job_docker` | Docker 构建参数解析、用户选项处理 | 构建日志中查看 Docker 参数 |
| dockercli | `pkg/dockercli` | Docker/Podman CLI 封装（镜像、容器操作） | 运行 `docker info` 或 `podman info` |

### 升级模块

| 模块 | 包路径 | 职责 | 确认正常的方法 |
|------|--------|------|-------------|
| upgrade | `pkg/upgrade` | Agent 内下载更新文件、触发 upgrader | Agent 日志中查看升级检查记录 |
| upgrader | `pkg/upgrader` | 独立二进制，负责替换 Agent/Daemon 文件并重启 | 升级后版本号变更 |

### 运维模块

| 模块 | 包路径 | 职责 | 确认正常的方法 |
|------|--------|------|-------------|
| agentcli | `pkg/agentcli` | CLI 子命令（install/uninstall/status/reinstall 等） | `devopsAgent -h` 查看帮助 |
| envs | `pkg/envs` | 环境变量管理（Windows 注册表轮询、API 下发） | Windows: 日志中查看注册表刷新记录 |
| cron | `pkg/cron` | 定期清理（日志、临时文件、调试容器） | 日志中查看清理记录 |
| collector | `pkg/collector` | 主机指标采集（CPU、内存等） | 心跳上报数据 |

### 扩展模块

| 模块 | 包路径 | 职责 | 确认正常的方法 |
|------|--------|------|-------------|
| imagedebug | `pkg/imagedebug` | 容器镜像交互式调试 | Docker 构建启用时自动可用 |
| pipeline | `pkg/pipeline` | 流水线任务执行（可选功能） | 配置 `devops.pipeline.enable=true` |
| mcp | `pkg/mcp` | MCP Server（为 AI 工具暴露 Agent 信息） | `status` 查看 MCP 端口 |

## 关键资源依赖

Agent 运行依赖以下资源文件，缺失时会影响构建：

| 资源 | 路径 | 用途 | 缺失时的影响 |
|------|------|------|------------|
| JDK 17 | `jdk17/` | 运行 `worker-agent.jar` 的首选 JDK | 回退到 JDK 8；都缺失则构建失败 |
| JDK 8 | `jdk/` | 兼容旧构建的备用 JDK | JDK 17 可用时无影响 |
| Worker | `worker-agent.jar` | 构建任务的实际执行者 | 构建失败，尝试从升级目录恢复 |
| Daemon | `devopsDaemon` | 守护进程 | Agent 异常退出后无法自动重启 |
| Docker Init | `docker_init.sh` | Docker 容器构建的入口脚本 | 首次 Docker 构建时自动下载 |

使用 `devopsAgent status` 命令可以一次性检查所有资源的状态。

## 并发控制

Agent 通过两个独立的计数器控制构建并发：

| 计数器 | 配置项 | 默认值 | 说明 |
|--------|--------|--------|------|
| 普通构建 | `devops.parallel.task.count` | 4 | 直接在宿主机上运行的构建任务数 |
| Docker 构建 | `devops.docker.parallel.task.count` | 4 | 在 Docker 容器中运行的构建任务数 |

这两个计数器互相独立，当任一计数器达到上限时，Agent 会在 Ask 请求中告知服务端暂不接受该类型的新任务。

升级期间会锁定所有构建槽位，等待运行中的任务完成后再执行升级。

## 通信机制

Agent 与服务端之间是**单向发起**的通信模型：

- Agent 主动发起 HTTP 请求到服务端网关
- 不需要服务端能访问到 Agent 机器
- 不需要 Agent 监听任何外部端口

所有通信通过 `X-DEVOPS-*` 系列 HTTP 头部进行身份认证：

| Header | 来源 |
|--------|------|
| `X-DEVOPS-PROJECT-ID` | `.agent.properties` 中的 `devops.project.id` |
| `X-DEVOPS-AGENT-ID` | `.agent.properties` 中的 `devops.agent.id` |
| `X-DEVOPS-AGENT-SECRET-KEY` | `.agent.properties` 中的 `devops.agent.secret.key` |

支持通过 HTTP/HTTPS 代理和自定义 TLS 证书访问服务端，详见 [配置参考](configuration.md)。

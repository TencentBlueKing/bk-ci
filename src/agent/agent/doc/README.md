# BK-CI 第三方构建机 Agent 用户文档

BK-CI Agent（又称"第三方构建机"）是一个部署在用户自有机器上的常驻程序，将该机器接入蓝鲸持续集成平台（BK-CI）作为构建节点。Agent 负责与 BK-CI 服务端通信、接收构建任务、管理 Worker 进程和 JDK 环境，并支持 Docker 容器化构建。

## 支持的平台与架构

| 操作系统 | CPU 架构 | 安装模式 |
|---------|---------|---------|
| Linux | amd64, arm64, loong64 | systemd 系统服务 / 用户级 systemd / 直接启动 |
| macOS | amd64 (Intel), arm64 (Apple Silicon)  | 直接启动 / launchd 无头模式 |
| Windows | x64, x86 | Windows 服务 / Session 桌面模式 / 计划任务（计划废弃,使用Session模式代替） |

## 核心能力

- **服务管理** — 一个二进制完成安装、卸载、启停
- **构建执行** — 接收服务端调度，启动 `worker-agent.jar` 执行流水线构建任务
- **Docker 构建** — 在 Docker / Podman 容器内执行构建，支持镜像调试
- **自动升级** — 服务端控制的无感知热升级（Agent、Daemon、Worker、JDK）
- **健康检查** — 内置网络、磁盘、等诊断，一条命令定位故障

## 快速入门

```bash
# 1. 从 BK-CI 平台"环境管理 → 节点 → 导入"页面复制安装命令
# 2. 在目标机器上，空目录中执行安装脚本
# 3. 验证安装状态
./devopsAgent status
```

## 文档索引

### 使用指南 (`guide/`)

| 文档 | 说明 |
|------|------|
| [安装部署指南](guide/installation.md) | 前置条件、各平台安装步骤、支持的部署场景 |
| [CLI 命令参考](guide/agent-cli.md) | 所有 CLI 子命令的用法和示例 |
| [架构与模块说明](guide/architecture.md) | Agent 整体架构、进程模型、模块职责、主循环流程 |
| [配置参考](guide/configuration.md) | `.agent.properties` 参数详解、辅助文件、环境变量开关 |
| [Linux 平台指南](guide/platform-linux.md) | systemd 模式、Root/非 Root、容器部署、login shell |
| [macOS 平台指南](guide/platform-macos.md) | launchd 模式、无头模式、JDK 路径差异 |
| [Windows 平台指南](guide/platform-windows.md) | 服务模式、Session 桌面模式、环境变量自动刷新 |
| [构建系统与工作空间](guide/build-and-workspace.md) | 构建流程、Worker 进程、JDK 管理、磁盘占用与清理 |
| [Docker/Podman 容器构建](guide/docker-builds.md) | 容器构建模式、Podman 兼容、挂载规则、镜像调试 |
| [升级与重装机制](guide/upgrade-and-reinstall.md) | 自动升级流程、`reinstall` 完全重装、保留文件清单 |
| [故障排查与状态检查](guide/troubleshooting.md) | `status` 命令解读、健康检查项、常见故障排查步骤 |

### 常见问题 (`faq/`)

| 文档 | 说明 |
|------|------|
| [macOS 构建环境变量缺失](faq/macos-env-missing.md) | launchd 启动导致 nvm/homebrew 等环境变量丢失的原因与解决 |

## 目录结构

Agent 安装后的典型目录结构：

```
<安装目录>/
├── devopsAgent                         # Agent 主程序（Linux/macOS）或 devopsAgent.exe（Windows）
├── devopsDaemon                        # 守护进程
├── worker-agent.jar                    # 构建 Worker（Java）
├── .agent.properties                   # 核心配置文件（身份信息、网关地址等）
├── .install_type                       # 当前安装模式标记
├── .cert                               # 自定义 TLS 证书（可选）
├── .debug                              # 调试模式开关文件（可选）
├── jdk17/                              # JDK 17 运行时
├── jdk/                                # JDK 8 运行时（兼容旧构建）
├── logs/                               # Agent 运行日志
├── runtime/                            # PID 文件等运行时状态
├── workspace/                          # 构建工作空间（代码检出、产物存放）
├── build_tmp/                          # 构建临时文件（自动清理）
├── install.sh | insatll.bat            # 安装脚本（兼容）
├── start.sh | start.bat                # 启动脚本（兼容）
├── uninstall.sh | uninstall.bat        # 卸载脚本（兼容）
└── stop.sh | stop.bat                  # 停止脚本（兼容）
```

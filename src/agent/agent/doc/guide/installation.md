# 安装部署指南

本文档介绍如何将 BK-CI Agent（第三方构建机）部署到各类环境中。

## 前置条件

| 条件 | 说明 |
|------|------|
| 网络 | 机器能够访问 BK-CI 服务端网关地址（默认 HTTP/HTTPS 端口） |
| 磁盘 | 建议预留 5GB 以上空间（Agent 自身 + JDK + 构建工作空间） |
| 权限 | Linux/macOS 建议使用专属构建用户安装；macOS root 用户安装时自动使用 DAEMON 模式（LaunchDaemons/system 域）；非 root 也可使用 |
| Java | 不需要预装 — Agent 自带 JDK 17 和 JDK 8 |
| Docker | 仅在使用 Docker 构建时需要，确保 `docker` 或 `podman` 命令可用 |

## 支持的部署场景

| 场景 | Linux | macOS | Windows |
|------|-------|-------|---------|
| 物理机 / 虚拟机 | ✓ | ✓ | ✓ |
| Docker 容器 | ✓ | — | — |
| Kubernetes Pod | ✓ | — | — |
| Root 用户 | ✓ (systemd) | ✓ (daemon 模式, /Library/LaunchDaemons) | ✓ (管理员) |
| 非 Root 用户 | ✓ (direct/user) | ✓ (login) | ✓ |
| 有桌面环境 | ✓ | ✓ | ✓ (Session 模式) |
| 无桌面 (headless) | ✓ | ✓ (background) | ✓ (service 模式) |

## 支持的 CPU 架构

| 操作系统 | 架构 |
|---------|------|
| Linux | amd64, arm64 (aarch64), loong64 |
| macOS | amd64 (Intel), arm64 (Apple Silicon) |
| Windows | x64, x86 |

安装脚本和 `reinstall` 命令会自动检测当前机器架构，下载对应的安装包。

## 首次安装

首次安装需要通过 BK-CI 平台获取安装命令，因为此时 Agent 二进制尚不存在。

### 步骤 1：获取安装命令

1. 登录 BK-CI 平台
2. 进入 **环境管理 → 节点 → 导入第三方构建机**
3. 选择目标操作系统，复制安装命令

### 步骤 2：执行安装

#### Linux

```bash
# 在目标机器上创建安装目录并执行，必须是空目录
mkdir -p /data/bkci-agent && cd /data/bkci-agent
<安装命令>
```

安装脚本会自动完成：
1. 下载 `agent.zip`（根据 CPU 架构自动选择对应包）
2. 解压得到 `devopsAgent`、`devopsDaemon`、`worker-agent.jar`、JDK 等
3. 调用 `./devopsAgent install` 注册系统服务

#### macOS

```bash
# 以普通用户安装（推荐）：使用 login 或 background 模式
mkdir -p ~/bkci-agent && cd ~/bkci-agent
<安装命令>

# 以 root 用户安装：自动切换为 DAEMON 模式（/Library/LaunchDaemons）
# 注意：DAEMON 模式无法访问 Keychain / Simulator / 桌面 UI
sudo mkdir -p /data/bkci-agent && cd /data/bkci-agent
<安装命令>
```

#### Windows

```powershell
# 以管理员身份打开 PowerShell
mkdir D:\bkci-agent; cd D:\bkci-agent
<安装命令>
```

### 步骤 3：验证安装

```bash
./devopsAgent status
```

正常输出应显示：
- Agent PID：运行中
- Daemon PID：运行中
- JDK 17：正常
- worker-agent.jar：正常
- 健康检查各项通过

## 安装模式

Agent 支持多种安装模式，通过 `install [<MODE>]` 指定（省略时使用平台默认）。不同平台的可用模式不同，详见各平台指南：

- [Linux 平台指南](platform-linux.md) — `service` / `user` / `direct`
- [macOS 平台指南](platform-macos.md) — `login` / `background`
- [Windows 平台指南](platform-windows.md) — `service` / `session` / `task`

完整的 CLI 命令参考见 [CLI 命令参考](agent-cli.md)。

## 容器内部署

### Docker 容器

在 Docker 容器内运行 Agent 时，由于没有 systemd，Agent 会自动使用 `direct` 模式：

```bash
# 容器入口脚本示例
cd /data/bkci-agent
./devopsAgent install direct
```

需要注意：
- 容器内没有 init 系统，Agent 和 Daemon 以前台方式运行
- 确保容器不会被频繁重建，否则每次都需要重新注册

### Kubernetes

建议通过 Deployment 或 StatefulSet 部署，将 Agent 安装目录挂载到持久卷（PVC），以保留身份信息和构建工作空间。

## 安装后的管理

首次安装完成后，所有管理操作使用 Agent 自带的 CLI：

```bash
./devopsAgent start       # 启动
./devopsAgent stop        # 停止
./devopsAgent uninstall   # 卸载
./devopsAgent status      # 查看状态
./devopsAgent reinstall   # 完全重装
./devopsAgent repair      # 修复（重新解压 JDK 等依赖）
```

详细命令说明见 [CLI 命令参考](agent-cli.md)。

## 防火墙与网络

Agent 作为客户端主动连接服务端，不需要监听外部端口。需要确保以下出站连接：

| 目标 | 用途 |
|------|------|
| BK-CI 网关 (`landun.gateway`) | 蓝盾流水线使用 |
| BK-CI 文件网关 (`landun.fileGateway`) | 蓝盾制品库使用 |

如果需要通过代理访问，可在 `.agent.properties` 中配置 `HTTP_PROXY`、`HTTPS_PROXY`、`NO_PROXY`，详见 [配置参考](configuration.md)。

## 卸载

```bash
# Linux / macOS
./devopsAgent uninstall

# Windows（管理员权限）
.\devopsAgent.exe uninstall
```

卸载会：
1. 停止 Agent 和 Daemon 进程
2. 移除系统服务注册（systemd unit / launchd plist / Windows 服务）
3. 清理 `.install_type` 标记
4. 保留 `.agent.properties` 和 `workspace/`，便于重新安装

如需彻底清除:
- 执行uninstall命令后，手动删除安装目录
- 在环境管理删除对应节点

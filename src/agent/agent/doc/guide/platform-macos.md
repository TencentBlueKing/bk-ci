# macOS 平台指南

本文档介绍 BK-CI Agent 在 macOS 平台上的安装模式、服务管理和注意事项。

## 安装模式

macOS 支持两种安装模式：

| 模式 | 命令 | 适用场景 |
|------|------|---------|
| `login` | `./devopsAgent install` | 默认模式，launchd gui 域，适合有桌面环境的机器 |
| `background` | `./devopsAgent install background` | launchd 无头模式，适合 SSH/CI/无桌面环境 |

两种模式都通过 launchd 管理服务生命周期，区别在于 launchd domain：
- `login` → `gui/{UID}`（需要桌面会话）
- `background` → `user/{UID}`（无头模式，类似 GitHub Actions Runner）
- Root → `system`

`install`/`start` 时自动快照当前 shell 的 PATH 和常用环境变量到 `.path`/`.env` 文件，daemon 启动时加载，解决 launchd 极简环境下构建进程找不到开发工具的问题。

### Login 模式（默认）

通过 launchd gui 域管理的服务，需要桌面会话：

```bash
./devopsAgent install   # login 为默认模式
```

特点：
- 通过 `launchctl bootstrap gui/{UID}` 注册到 launchd
- 需要用户登录桌面会话才能运行
- 适合开发者在本地 Mac 上使用
- 构建进程可以访问桌面 UI（如 Xcode UI 测试）

> **建议**：为确保 Mac 构建机重启后 Agent 能自动恢复运行，请在 **系统设置 > 用户与群组** 中开启 **自动以此身份登录**。否则重启后需手动登录桌面才能恢复。

### Background 模式

通过 launchd user 域管理的无头服务：

```bash
./devopsAgent install background
```

特点：
- 通过 `launchctl bootstrap user/{UID}` 注册到 launchd
- 适合 SSH 远程管理的 Mac 构建机（如 Mac mini 机架）
- 不依赖用户桌面登录
- `RunAtLoad=true` 支持开机自启

launchd plist 文件位置：
- 非 Root：`~/Library/LaunchAgents/devops_agent_{id}.plist`
- Root：`/Library/LaunchDaemons/devops_agent_{id}.plist`

启停命令：
```bash
# 使用 Agent CLI（自动选择正确方式）
./devopsAgent start
./devopsAgent stop

# 或使用 launchctl
launchctl bootout gui/$(id -u)/devops_agent_{id}
```

## JDK 路径差异

macOS 上 JDK 的目录结构与 Linux 不同，Java 可执行文件在 `Contents/Home/bin/` 下：

| 平台 | JDK 17 Java 路径 | JDK 8 Java 路径 |
|------|------------------|-----------------|
| Linux | `jdk17/bin/java` | `jdk/bin/java` |
| macOS | `jdk17/Contents/Home/bin/java` | `jdk/Contents/Home/bin/java` |

Agent 会自动处理这个差异，用户通常不需要关心。如果在 `.agent.properties` 中自定义 JDK 路径，需要注意填写到 `Contents/Home` 这一层的父目录。

## 构建脚本与 Login Shell

与 Linux 相同，macOS 上的构建脚本也使用 login shell（`-l` 标志）启动，确保加载 `~/.bash_profile`、`~/.zprofile` 等初始化文件。

macOS 的默认 shell 说明：
- macOS Catalina (10.15) 及以后版本默认 shell 为 `zsh`
- 如果 `devops.agent.detect.shell=true`，Agent 会使用 `$SHELL`（通常是 `/bin/zsh`）
- 如果 `devops.agent.detect.shell=false`（默认），使用 `/bin/bash`

## 进程组管理

macOS arm64（Apple Silicon）上，`DEVOPS_AGENT_ENABLE_EXIT_GROUP` 默认为 `true`。这是因为 Apple Silicon 上的进程管理行为与 Intel 有差异，启用进程组清理可以确保构建结束后所有子进程被正确回收。

在 Intel Mac 上此功能默认关闭，可通过环境变量手动启用：
```bash
export DEVOPS_AGENT_ENABLE_EXIT_GROUP=true
```

## 注意事项

### Gatekeeper 和签名

首次运行 Agent 二进制时，macOS 可能会弹出安全警告：
- 方法 1：在"系统偏好设置 → 安全性与隐私"中允许运行
- 方法 2：使用 `xattr -d com.apple.quarantine devopsAgent devopsDaemon` 移除隔离标记

### 自动登录（Login 模式推荐）

Login 模式要求用户登录桌面才能运行。如果 Mac 构建机需要在重启后自动恢复 Agent，建议开启 macOS 自动登录：

1. 打开 **系统设置 > 用户与群组**
2. 将 **自动以此身份登录** 设置为构建用户
3. 如果使用了 FileVault 磁盘加密，需要先关闭 FileVault 才能开启自动登录

如果无法配置自动登录（如安全策略不允许），建议使用 `background` 模式，该模式不依赖用户登录。

### 休眠与屏保

如果 Mac 构建机进入休眠状态，Agent 进程可能被挂起：
- 建议关闭自动休眠：`系统偏好设置 → 节能 → 阻止电脑自动进入睡眠`
- 或使用 `caffeinate` 命令防止休眠

### SSH 远程访问

通过 SSH 登录的 Mac 如果使用 `login` 模式安装，断开 SSH 后 Agent 仍会继续运行（因为是独立的后台进程）。如果需要更可靠的后台运行，建议使用 `background` 模式。

### Xcode 命令行工具

如果构建需要使用 Xcode 工具链，确保已安装：
```bash
xcode-select --install
```

## 支持的架构

| 架构 | 处理器 | 说明 |
|------|--------|------|
| amd64 | Intel | 传统 Intel Mac |
| arm64 | Apple Silicon (M1/M2/M3/M4) | 原生 arm64 二进制 |

安装脚本和 `reinstall` 命令会自动检测架构并下载对应的安装包。

## status 命令示例

```bash
$ ./devopsAgent status
[BK-CI] ============================================
[BK-CI] BK-CI Agent 状态
[BK-CI] ============================================
  平台:                    macOS
  工作目录:                 /Users/builder/bkci-agent
  服务名:                  devops_agent_abc123
  当前用户:                 builder
  安装模式:                 BACKGROUND
  运行模式:                 launchd background (无头模式)
  launchd 域:              gui/501
  plist 路径:              /Users/builder/Library/LaunchAgents/devops_agent_abc123.plist
  Daemon PID:             1234 (运行中)
  Agent PID:              5678 (运行中)
  JDK 17:                 正常 ✓
  JDK 8:                  正常 ✓
  worker-agent.jar:       正常 ✓ (45.2 MB)

  健康检查
  --------------------------------------------
  磁盘可用:                120.5 GB ✓
  磁盘可写:                正常 ✓

  网关 (devops.example.com):
    DNS resolve:          10.0.0.1 (2ms) ✓
    TCP connect:          10.0.0.1:443 (5ms) ✓
    TLS handshake:        TLS 1.3 (10ms) ✓
    HTTP GET:             200 OK (35ms) ✓
    Proxy:                直连 (无代理)
```

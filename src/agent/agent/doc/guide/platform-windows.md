# Windows 平台指南

本文档介绍 BK-CI Agent 在 Windows 平台上的安装模式、特殊行为和注意事项。

## 安装模式

Windows 支持三种安装模式：

| 模式 | 命令 | 适用场景 |
|------|------|---------|
| `service` | `devopsAgent.exe install` | 默认模式，后台服务运行，适合大多数场景 |
| `session` | `devopsAgent.exe install --mode session` | 需要访问桌面 UI 的构建（如 GUI 测试、UWP 打包） |
| `task` | `devopsAgent.exe install --mode task` | **已废弃**，计划任务模式，保留兼容 |

### Service 模式（默认）

Agent 以 Windows 服务方式运行，通过 `sc.exe` 注册：

```powershell
.\devopsAgent.exe install
.\devopsAgent.exe install --mode service   # 等同上面
```

特点：
- 服务名：`devops_agent_{id}`
- 开机自动启动（`start= auto`）
- 运行在 Session 0，**无法访问桌面**
- 使用 `sc.exe start/stop` 管理

### Session 模式

当构建需要与桌面交互时（例如运行 GUI 测试、访问剪贴板、使用 RDP），使用 Session 模式。

```powershell
# 基本 Session 模式（依赖当前已登录用户）
.\devopsAgent.exe install --mode session

# 带自动登录（重启/注销后自动恢复用户会话）
.\devopsAgent.exe install --mode session --auto-logon builduser P@ssw0rd
```

#### Session 参数

| 参数 | 说明 |
|------|------|
| `--auto-logon USER PASSWORD` | 配置 Windows 自动登录，注销/重启后自动恢复用户会话。用户名支持 `DOMAIN\user` 或 `user@domain` 格式，安装前会通过 LogonUser API 验证凭据 |

#### 行为对比

| 场景 | 无 `--auto-logon` | 有 `--auto-logon` |
|------|-------------------|-------------------|
| 当前有用户登录 | Daemon 通过 WTS API 在用户 Session 中启动 Agent | 同左 |
| 用户注销 | Agent 被终止，Daemon 等待用户重新登录后自动恢复 | Windows 自动登录 → 新用户会话 → Agent 自动恢复 |
| 系统重启 | 需等待用户手动登录 | Windows 自动登录 → Agent 自动恢复 |
| 锁屏 (Win+L) | Agent 不受影响，继续运行 | 同左 |

> **注意**：未配置 `--auto-logon` 时，注销后 Daemon 不会回退到 SYSTEM 身份启动 Agent，而是每 30 秒检测一次，等待用户登录后在用户会话中恢复。这避免了 SYSTEM 身份下执行构建可能带来的权限和环境问题。

#### 自动登录安全

- 自动登录密码通过 **LSA Secret**（Local Security Authority）加密存储
- 与 Sysinternals Autologon 使用相同的安全机制
- 不在任何配置文件中以明文出现
- `uninstall` 会自动清理自动登录配置

#### 密码变更

密码变更后需要重新安装（会自动先卸载旧配置）：

```powershell
.\devopsAgent.exe install --mode session --auto-logon builduser NewP@ssw0rd
```

### Task 模式（已废弃）

通过 Windows 计划任务（`schtasks`）管理 Agent，使用 `devopsctl.vbs` 隐藏窗口。此模式已废弃，建议迁移到 `service` 或 `session` 模式。

## 环境变量自动刷新

Windows 上，Agent 每 **3 秒** 自动轮询系统注册表，获取最新的环境变量：

- **用户级**：`HKEY_CURRENT_USER\Environment`
- **系统级**：`HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment`

合并规则遵循 Windows 标准（`PATH` 拼接，其他键用户级覆盖系统级）。

这意味着：
- 安装新软件后不需要重启 Agent，等待 3 秒即可在构建中使用新的环境变量
- 环境变量的变更会记录在 Agent 日志中（`add`/`update`/`delete`）
- 其他平台（Linux/macOS）没有此功能，需要重启 Agent 或通过 login shell 加载

## 进程管理

### 句柄继承

Windows 上构建进程默认设置 `NoInheritHandles`，防止 Agent/Daemon 的管道句柄泄漏到构建进程，避免构建结束后进程无法退出的问题。

### 新控制台

设置 `DEVOPS_AGENT_ENABLE_NEW_CONSOLE=true` 可以让每个构建进程在新的控制台窗口中运行，便于调试。

## 注意事项

### 文件锁

Windows 不允许删除/覆盖正在运行的可执行文件：
- `reinstall` 命令会自动将运行中的 `devopsAgent.exe` 重命名为 `.bak` 后再解压新文件
- 如果手动操作遇到"拒绝访问"错误，先停止 Agent 再操作

### 防火墙

Agent 作为客户端主动连接服务端，不需要监听入站端口。确保出站连接到 BK-CI 网关畅通即可。

### 权限

- `service` 和 `session` 模式需要管理员权限安装
- Agent 服务默认以 `LocalSystem` 账户运行
- Session 模式可以指定运行用户

## status 命令示例

```powershell
PS> .\devopsAgent.exe status
[BK-CI] ============================================
[BK-CI] BK-CI Agent 状态
[BK-CI] ============================================
  平台:                    Windows
  工作目录:                 D:\bkci-agent
  服务名:                  devops_agent_abc123
  当前用户:                 SYSTEM
  安装模式:                 SESSION
  运行模式:                 Windows 服务 + 桌面 Session
  服务状态:                 RUNNING
  自动登录:                 已启用 (DefaultUserName=builduser)
  Daemon PID:             1234 (运行中)
  Agent PID:              5678 (运行中)
  JDK 17:                 正常 ✓
  JDK 8:                  正常 ✓
  worker-agent.jar:       正常 ✓ (45.2 MB)

  健康检查
  --------------------------------------------
  磁盘可用:                15.3 GB ✓
  磁盘可写:                正常 ✓

  网关 (devops.example.com):
    DNS resolve:          10.0.0.1 (2ms) ✓
    TCP connect:          10.0.0.1:443 (5ms) ✓
    TLS handshake:        TLS 1.3 (12ms) ✓
    HTTP GET:             200 OK (50ms) ✓
    Proxy:                直连 (无代理)
```

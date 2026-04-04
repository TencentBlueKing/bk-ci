# BK-CI Agent CLI 使用指南

Agent 二进制（`devopsAgent`）内置了完整的服务管理 CLI，替代之前分散的安装脚本。

## 命令总览

```
devopsAgent <command> [options]

服务管理:
  install [--mode <MODE>]   安装并启动 daemon (各平台支持不同模式，见下方说明)
  uninstall                 停止并卸载 daemon 服务
  start                     启动 daemon (根据安装模式自动选择启动方式)
  stop                      停止 daemon (根据安装模式自动选择停止方式)

维护:
  repair                    停止 → 重新解压 JDK/依赖 → 重启
  reinstall [-y]            完全重装: 保留身份, 从服务端重新下载
  status                    运行状态 + 健康检查 (网络/磁盘，末尾附汇总)

调试:
  debug [on|off]            切换调试模式 (修改后需重启 Agent 生效)
  version [-f]              打印版本号 (-f 显示完整信息)
  -h, --help                显示帮助
  (无参数)                   直接运行 agent
```

---

## install

注册 daemon 并启动。自动处理 JDK 解压和目录创建。

### 安装模式（--mode）

所有平台通过 `--mode` 参数选择安装模式。不指定时使用平台默认模式。

| 平台 | 可选模式 | 默认 | 说明 |
|------|---------|------|------|
| **Linux** | `service` / `user` / `direct` | root: `service`; 非 root: `direct` | `service` = 系统级 systemd; `user` = 用户级 systemd; `direct` = 直接启动 |
| **macOS** | `login` / `background` | `login` | `login` = 直接启动, 需登录桌面; `background` = launchd 无头模式 |
| **Windows** | `service` / `session` / `task` | `service` | `service` = Windows 服务; `session` = 服务 + 桌面会话; `task` = [计划废弃] 计划任务 |

### 模式自动切换

- 目标模式与当前已安装模式**相同** → 跳过，提示已安装
- 目标模式与当前已安装模式**不同** → 自动先执行 `uninstall`，再安装新模式

```bash
# 首次安装（root 默认 service 模式）
sudo ./devopsAgent install

# 切换模式：无需手动 uninstall，install 自动检测并切换
./devopsAgent install --mode user

# 重复执行相同模式：不会重复安装
./devopsAgent install --mode user
# => "Agent is already installed in USER mode. Nothing to do."
```

### Linux 示例

```bash
# root 用户 — 默认安装为系统级 systemd 服务
sudo ./devopsAgent install
sudo ./devopsAgent install --mode service   # 同上

# 非 root 用户 — 默认直接启动
./devopsAgent install
./devopsAgent install --mode direct   # 同上

# 非 root 用户 — 用户级 systemd (注销后仍运行)
./devopsAgent install --mode user
# 自动尝试 loginctl enable-linger，失败会打印手动命令
```

### macOS 示例

```bash
# 默认 login 模式（需用户登录桌面）
./devopsAgent install
./devopsAgent install --mode login   # 同上

# 无头模式（SSH/无桌面环境可用）
./devopsAgent install --mode background
```

### Windows 示例

```powershell
# 默认服务模式
.\devopsAgent.exe install
.\devopsAgent.exe install --mode service   # 同上

# 服务 + Session 模式（一步到位）
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd --auto-logon
```

---

## uninstall

停止服务、移除服务注册、清理残留进程和 `.install_type` 标记。

```bash
./devopsAgent uninstall            # Linux/macOS
.\devopsAgent.exe uninstall        # Windows (管理员)
```

---

## start / stop

根据 `.install_type` 标记自动选择对应的启停方式。

```bash
./devopsAgent start
./devopsAgent stop
```

各平台行为：

| 安装模式 | start | stop |
|---------|-------|------|
| Linux `SERVICE` | `systemctl start` | `systemctl stop` |
| Linux `USER` | `systemctl --user start` | `systemctl --user stop` |
| Linux `DIRECT` | 直接启动 daemon | 通过 PID 文件杀进程 |
| macOS `LOGIN` | 直接启动 daemon | 通过 PID 文件杀进程 |
| macOS `BACKGROUND` | `launchctl bootstrap` + `kickstart` | `launchctl bootout` |
| Windows | `sc.exe start` | `sc.exe stop` |

---

## Session 模式 (Windows)

当 daemon 以 Windows 服务运行时，默认在 Session 0（无桌面访问）。Session 模式让 agent 及其构建进程能访问用户桌面。

通过 `install --mode session` 配置：

```powershell
# 最简单：依赖当前已登录用户的 Session
.\devopsAgent.exe install --mode session

# 配置 LogonUser：没人登录时也能创建 Session
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd

# 完整：配置 Windows 自动登录，重启后会自动登录，也有 Session
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd --auto-logon
```

| 参数 | 说明 |
|------|------|
| `--user` | Windows 登录账号（可选，支持 `DOMAIN\user` 或 `user@domain` 格式） |
| `--password` | 账号密码（指定 `--user` 时必填，配置前会通过 LogonUser API 验证） |
| `--auto-logon` | 配置 Windows 自动登录（系统级设置，每次重启自动登录，需要 `--user`） |

### 各层级效果对比

| 场景 | 无参数 | 有凭据 | 凭据 + --auto-logon |
|------|--------|--------|---------------------|
| 当前有用户登录 | daemon 通过 WTS API 在用户 Session 中启动 agent | 同左 | 同左 |
| 没人登录 | agent 回退到 Session 0 | daemon 用 LogonUser 在控制台 Session 创建进程 | Windows 自动登录 → 产生 Session |
| 系统影响 | 无 | 凭据加密存储在 LSA Secret | 同左 + 修改注册表自动登录 |

### 密码变更

如果 Windows 账号密码变更，需要用新密码重新运行 install（会自动 uninstall 旧配置再安装）：

```powershell
.\devopsAgent.exe install --mode session --user builduser --password NewP@ssw0rd
```

### 凭据安全

- Session 凭据通过 **LSA Secret** (Local Security Authority) 加密存储
- 不在任何配置文件中以明文出现
- `uninstall` 会清理所有凭据和自动登录配置

---

## Linux 用户级 systemd 模式

`--mode user` 将 agent 安装为用户级 systemd 服务，适合非 root 用户且希望注销后服务继续运行的场景。

```bash
./devopsAgent install --mode user
```

- systemd unit 文件位于 `~/.config/systemd/user/`
- 使用 `systemctl --user` 管理
- 需要 `loginctl enable-linger` 才能在注销后保持运行
- install 时会自动尝试启用 linger，失败则打印手动命令

`status` 命令会显示 linger 状态：

```
  Install mode:            USER
  Run mode:                user systemd (survives logout with linger)
  Service state:           active
  Auto start:              enabled
  Linger:                  enabled (survives logout) ✓
```

---

## 各平台服务注册方式

| 平台 | 条件 | 注册方式 | 服务名 |
|------|------|---------|--------|
| Linux | root + systemd (`--mode service`) | `/etc/systemd/system/devops_agent_{id}.service` | `devops_agent_{id}` |
| Linux | 非 root + systemd (`--mode user`) | `~/.config/systemd/user/devops_agent_{id}.service` | `devops_agent_{id}` |
| Linux | `--mode direct` 或无 systemd | 直接后台启动 daemon | 无 |
| macOS | `--mode login` | 直接后台启动 daemon | 无 |
| macOS | `--mode background` (非 root) | `~/Library/LaunchAgents/devops_agent_{id}.plist` | `devops_agent_{id}` |
| macOS | root | `/Library/LaunchDaemons/devops_agent_{id}.plist` | `devops_agent_{id}` |
| Windows | `--mode service` | Windows 服务 (`sc.exe create`) | `devops_agent_{id}` |
| Windows | `--mode session` | Windows 服务 + LSA Secret 凭据 | `devops_agent_{id}` |

---

## 旧脚本兼容性

Agent 支持自动升级：agent 二进制更新为新版本，但用户机器上可能仍保留旧版安装脚本。

### 旧脚本操作新 Agent

所有旧脚本（`start.sh`、`stop.sh`、`uninstall.sh`、`install.sh` 等）**不调用 `devopsAgent` CLI**，
而是直接操作系统服务管理器（systemd/launchd/sc.exe）和进程（kill/nohup），
因此与新版 agent 二进制**完全兼容**，无需做任何修改。

### 新旧 systemd unit 格式差异

旧脚本创建的 systemd unit 使用 `Type=forking` + `ExecStart=start.sh`，
新 CLI 创建的使用 `Type=simple` + `ExecStart=devopsDaemon`。
两种格式都能正常工作。用户后续运行 `devopsAgent install` 时会自动替换为新格式。

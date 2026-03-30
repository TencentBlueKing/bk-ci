# BK-CI Agent CLI 使用指南

Agent 二进制（`devopsAgent`）内置了服务管理和 Session 配置的 CLI 子命令，替代之前分散的安装脚本。

## 命令总览

```
devopsAgent <command> [options]

服务管理:
  install [options]    安装并启动 daemon
    --mode service     (默认) 安装为 Windows 服务
    --mode session     安装为服务 + 配置桌面会话 (一步到位)
    --mode task        [已废弃] 安装为计划任务
  uninstall            停止并卸载 daemon 服务
  start                启动 daemon
  stop                 停止 daemon

维护:
  repair               停止 → 重新解压 JDK/依赖 → 重启
  reinstall [-y]       完全重装: 保留身份, 从服务端重新下载
  status               运行状态 + 健康检查 (网络/磁盘/证书诊断，末尾附汇总)

Session 模式 (仅 Windows):
  configure-session    配置桌面 Session 访问

其他:
  version              打印版本号
    -f                 打印完整版本信息 (版本号 / Git Commit / 构建时间)
  -h, --help           显示帮助
  (无参数)             正常运行 agent
```

---

## 服务管理

### install

注册 daemon 并启动。自动处理 JDK 解压和目录创建。Windows 支持 `--mode` 选择安装模式。

```bash
# Linux / macOS (--mode 参数忽略，始终使用系统服务)
sudo ./devopsAgent install

# Windows - 默认服务模式
.\devopsAgent.exe install
.\devopsAgent.exe install --mode service   # 同上

# Windows - 服务 + Session 模式 (一步到位，替代 install + configure-session)
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd
.\devopsAgent.exe install --mode session --user builduser --password P@ssw0rd --auto-logon

# Windows - 计划任务模式 [已废弃，建议用 session]
.\devopsAgent.exe install --mode task
```

### uninstall

停止服务、移除服务注册、清理残留进程。Windows 上额外清理 Session 模式配置。

```bash
sudo ./devopsAgent uninstall       # Linux/macOS
.\devopsAgent.exe uninstall        # Windows (管理员)
```

### start / stop

```bash
./devopsAgent start
./devopsAgent stop
```

各平台行为：
- **Linux (有 systemd unit)**：走 `systemctl start/stop`
- **Linux (无 systemd)**：直接启动 daemon / 通过 PID 文件杀进程
- **macOS**：走 `launchctl load/unload`
- **Windows**：走 `sc.exe start/stop`

---

## Session 模式 (Windows)

当 daemon 以 Windows 服务运行时，默认在 Session 0（无桌面访问）。Session 模式让 agent 及其构建进程能访问用户桌面。

### 三种配置层级

```powershell
# 1. 最简单：依赖当前已登录用户的 Session
.\devopsAgent.exe configure-session

# 2. 配置 LogonUser 回退：没人登录时也能创建 Session
.\devopsAgent.exe configure-session --user builduser --password P@ssw0rd

# 3. 完整：配置 Windows 自动登录，重启后也有 Session
.\devopsAgent.exe configure-session --user builduser --password P@ssw0rd --auto-logon

# 取消 Session 模式
.\devopsAgent.exe configure-session --disable
```

### 配置说明

| 参数 | 说明 |
|------|------|
| `--user` | Windows 登录账号（可选，支持 `DOMAIN\user` 或 `user@domain` 格式） |
| `--password` | 账号密码（指定 `--user` 时必填，配置前会通过 LogonUser API 验证） |
| `--auto-logon` | 配置 Windows 自动登录（系统级设置，每次重启自动登录，需要 `--user`） |
| `--disable` | 取消 Session 模式，清理 LSA Secret 和自动登录配置 |

命令完成后的摘要提示会根据系统语言自动切换中英文。Windows CLI 启动时会尝试切换控制台到 UTF-8，以尽量减少中文乱码。

### 各层级效果对比

| 场景 | 无参数 | 有凭据 | 凭据 + --auto-logon |
|------|--------|--------|---------------------|
| 当前有用户登录 | daemon 通过 WTS API 在用户 Session 中启动 agent | 同左 | 同左 |
| 没人登录 | agent 回退到 Session 0 | daemon 用 LogonUser 在控制台 Session 创建进程 | Windows 自动登录 → 产生 Session |
| 系统影响 | 无 | 凭据加密存储在 LSA Secret | 同左 + 修改注册表自动登录 |

### 密码变更

如果 Windows 账号密码变更，需要用新密码重新运行：

```powershell
.\devopsAgent.exe configure-session --user builduser --password NewP@ssw0rd
```

脚本会先验证密码是否正确，错误时直接报错不会存入。

### 凭据安全

- Session 凭据通过 **LSA Secret** (Local Security Authority) 加密存储
- 与 Sysinternals Autologon 和 Azure DevOps Agent 使用相同的安全机制
- 不在任何配置文件中以明文出现
- `uninstall` 和 `configure-session --disable` 会清理所有凭据

---

## 安装流程

### 首次安装

首次安装仍使用下载脚本引导（因为此时 agent 二进制尚不存在）：

```bash
# Linux / macOS
curl -o install.sh "https://your-server/install.sh" && bash install.sh

# Windows
Invoke-WebRequest -Uri "https://your-server/download_install.ps1" -OutFile dl.ps1; .\dl.ps1
```

### 安装后管理

首次安装完成后，所有管理操作使用 agent CLI：

```bash
./devopsAgent install      # 安装/重装服务
./devopsAgent uninstall    # 卸载
./devopsAgent start        # 启动
./devopsAgent stop         # 停止
```

### 典型运维流程

```bash
# 卸载后重装
./devopsAgent uninstall
./devopsAgent install

# 文件损坏修复（JDK 被误删等）— 一条命令搞定
./devopsAgent repair
# 等价于: stop → 删除 jdk17/jdk 目录 → 从 zip 重新解压 → start

# 切换到 Session 模式 (Windows)
.\devopsAgent.exe configure-session --user builduser --password xxx

# 切换到 Session + 自动登录 (Windows)
.\devopsAgent.exe configure-session --user builduser --password xxx --auto-logon

# 回退到普通模式 (Windows)
.\devopsAgent.exe configure-session --disable
```

---

## 各平台服务注册方式

| 平台 | 条件 | 注册方式 | 服务名 |
|------|------|---------|--------|
| Linux | root + systemd | `/etc/systemd/system/devops_agent_{id}.service` | `devops_agent_{id}` |
| Linux | root + 无 systemd | 直接后台启动 daemon（容器场景） | 无 |
| Linux | 非 root | 直接后台启动 daemon | 无 |
| macOS | root | `/Library/LaunchDaemons/devops_agent_{id}.plist` | `devops_agent_{id}` |
| macOS | 非 root | `~/Library/LaunchAgents/devops_agent_{id}.plist` | `devops_agent_{id}` |
| Windows | 管理员 | Windows 服务 (`sc.exe create`) | `devops_agent_{id}` |

---

## 脚本清理

`upgrader` 和引导脚本已全部改为调用 `devopsAgent install/uninstall`，
原有的管理脚本和 `installer` 组件已废弃删除：

| 已删除脚本 | 替代命令 |
|-----------|---------|
| `install.bat` / `install.sh`（安装服务部分） | `devopsAgent install` |
| `uninstall.bat` / `uninstall.sh` | `devopsAgent uninstall` |
| `start.bat` / `start.sh` | `devopsAgent start` |
| `stop.bat` / `stop.sh` | `devopsAgent stop` |
| `configure_session.ps1` | `devopsAgent configure-session` |
| `install_schtasks_for_ui.bat` | `devopsAgent configure-session` |
| `devopsctl.vbs` | 不再需要 |

保留的脚本（仅用于首次下载引导，此时 agent 二进制尚不存在）：

| 保留脚本 | 用途 |
|---------|------|
| `install.sh`（Linux/macOS） | 首次下载 agent.zip + 解压 + 调用 `devopsAgent install` |
| `download_install.ps1`（Windows） | 首次下载 agent.zip + 解压 + 调用 `devopsAgent install` |
| `agent_docker_init.sh`（Linux） | Docker 容器初始化 |

---

## Docker Runtime

Docker 构建相关逻辑已改为**优先调用容器运行时命令行**，默认使用 `docker`，可通过环境变量切换为 `podman`：

```bash
export DEVOPS_AGENT_CONTAINER_RUNTIME=podman
```

设计说明：

- Agent 在构建、镜像调试容器创建/启动/等待/日志、调试容器清理等场景下，统一通过容器命令行执行
- 会将完整命令和 stdout/stderr 记录到 Agent 日志；Docker 构建任务还会将命令和关键输出上报到构建日志
- 这样可以避免 Go 版本与 Docker SDK 版本绑定导致的 API 兼容问题
- 镜像调试的 WebSocket `exec` 交互终端也已改为 `docker/podman exec` + PTY 转发，不再依赖 Docker SDK

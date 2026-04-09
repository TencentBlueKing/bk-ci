# Linux 平台指南

本文档介绍 BK-CI Agent 在 Linux 平台上的安装模式、服务管理和特殊行为。

## 安装模式

Linux 支持三种安装模式：

| 模式 | 命令 | 适用场景 |
|------|------|---------|
| `service` | `sudo ./devopsAgent install` | Root 用户默认，系统级 systemd 服务 |
| `user` | `./devopsAgent install user` | 非 Root 用户的 systemd 服务，注销后仍运行 |
| `direct` | `./devopsAgent install direct` | 非 Root 默认，直接后台启动，无服务注册 |

### Service 模式（Root 默认）

以系统级 systemd 服务运行，开机自动启动：

```bash
sudo ./devopsAgent install
```

systemd 单元文件位置：`/etc/systemd/system/devops_agent_{id}.service`

单元文件关键配置：
- `Type=simple` — Agent 进程直接由 systemd 管理
- `ExecStart=<安装目录>/devopsDaemon` — 启动守护进程
- `ExecStartPre` — 清理残留 PID 文件
- `KillMode=none` — 避免 systemd 杀死升级中的进程
- `PrivateTmp=false` — 允许访问 `/tmp`

环境变量处理：
- `install` 和 `start` 命令执行时，Agent 会将当前 shell 的 PATH 和常用开发变量（`JAVA_HOME`、`GOROOT` 等）快照到 `.path` 和 `.env` 文件
- daemon 启动后读取这些文件并合并到进程环境，解决 systemd 极简环境下构建进程找不到开发工具的问题
- 修改 `.bashrc`/`.profile` 后，执行 `stop` + `start` 即可使变更生效

启停命令：
```bash
sudo systemctl start devops_agent_{id}
sudo systemctl stop devops_agent_{id}
sudo systemctl status devops_agent_{id}

# 或使用 Agent CLI（自动选择正确方式）
./devopsAgent start
./devopsAgent stop
```

### User 模式

非 Root 用户的 systemd 用户级服务，适合需要注销后仍保持运行的场景：

```bash
./devopsAgent install user
```

特点：
- 单元文件位于 `~/.config/systemd/user/devops_agent_{id}.service`
- 使用 `systemctl --user` 管理
- 安装时自动尝试 `loginctl enable-linger`（允许注销后服务继续运行）
- 如果 linger 启用失败，会打印手动启用命令

启停命令：
```bash
systemctl --user start devops_agent_{id}
systemctl --user stop devops_agent_{id}

# 或使用 Agent CLI
./devopsAgent start
./devopsAgent stop
```

`status` 命令会显示 linger 状态：
```
  安装模式:     USER
  运行模式:     用户级 systemd (启用 linger 后注销仍运行)
  服务状态:     active
  自动启动:     enabled
  Linger:      已启用 (注销后仍运行) ✓
```

#### LDAP/域账号限制

如果当前用户是通过 LDAP、NIS 或 SSSD 管理的网络/目录账号（而非 `/etc/passwd` 中的本地用户），**linger 的开机自启功能可能不生效**。

**现象**：linger 已启用（`status` 显示 `Linger: enabled ✓`），但系统重启后 Agent 未自动拉起，直到用户通过 SSH 或桌面登录后才恢复。

**原因**：系统启动早期，`systemd-logind` 尝试为 linger 用户启动 `user@UID.service`，但此时负责用户解析的 `nslcd`/`sssd` 服务尚未就绪，导致用户名无法解析，logind 跳过该用户。日志表现为：

```
systemd-logind: Couldn't add lingering user <username>, ignoring: No such file or directory
```

**诊断**：

```bash
# 检查系统启动时 logind 是否报错
sudo journalctl -b -u systemd-logind | grep -i linger

# 确认用户是否在本地 passwd 中
grep "^$(whoami):" /etc/passwd
```

如果 `grep` 无输出，说明当前用户是网络账号。Agent 在 `install user` 时也会自动检测并打印警告。

**解决方案**：使用 root 权限安装为系统级 systemd 服务，不依赖用户解析：

```bash
sudo ./devopsAgent install
```

### Direct 模式（非 Root 默认）

直接后台启动 Daemon 进程，不注册任何系统服务：

```bash
./devopsAgent install
./devopsAgent install direct   # 非 Root 时的默认
```

特点：
- 适合容器内部署、临时测试等场景
- 没有开机自启能力
- 通过 PID 文件 (`runtime/daemon.pid`) 管理进程

## Root 与非 Root 的差异

| 方面 | Root | 非 Root |
|------|------|---------|
| 默认安装模式 | `service`（systemd 系统级） | `direct`（直接启动） |
| 可选模式 | `service` / `direct` | `user` / `direct` |
| systemd 单元位置 | `/etc/systemd/system/` | `~/.config/systemd/user/` |
| 切换用户构建 | 可通过 `devops.slave.user` 指定构建用户 | 只能以当前用户构建 |
| 无 systemd 环境 | 自动回退到 `direct` 并警告 | 默认就是 `direct` |

## 构建脚本与 Login Shell

Agent 在 Linux 上启动构建进程时使用**两层脚本嵌套**（"套娃"模式）：

```
prepare_script.sh (外层)
  └── exec $SHELL -l start_script.sh (内层, login shell)
        └── java -jar worker-agent.jar (构建进程)
```

**`-l` 标志的作用**：以 login shell 方式运行，会加载用户的 `~/.bash_profile`、`~/.profile` 等初始化文件，确保构建进程能获取到用户配置的环境变量（如 `JAVA_HOME`、`PATH` 修改等）。

Shell 选择逻辑：
- `devops.agent.detect.shell=true` → 使用 `$SHELL` 环境变量指定的 shell
- `devops.agent.detect.shell=false`（默认）→ 使用 `/bin/bash`
- 特殊处理：如果 shell 是 `tcsh`，参数顺序会调整为 `exec tcsh script -l`

## 进程隔离

### 进程组管理

设置 `DEVOPS_AGENT_ENABLE_EXIT_GROUP=true` 后，Agent 会：
1. 构建进程启动时设置 `Setpgid=true`（独立进程组）
2. 构建结束后向整个进程组发送 `SIGKILL`
3. 确保构建派生的所有子进程都被清理

### 文件描述符隔离

设置 `DEVOPS_AGENT_CLOSE_FD_INHERIT=true` 后，构建进程不会继承 Agent/Daemon 的管道文件描述符：
- `Setpgid=true`（进程组隔离）
- `Stdin`/`Stdout`/`Stderr` 重定向到 `/dev/null`
- `ExtraFiles` 清空

这可以防止构建进程因持有 Daemon 管道而无法退出的问题。

## 容器内部署

在 Docker 容器或 Kubernetes Pod 中运行 Agent 时的注意事项：

1. **没有 systemd** — Agent 自动使用 `direct` 模式
2. **PID 1 问题** — 建议使用 `tini` 或类似工具作为 init 进程
3. **持久化** — 将以下文件/目录挂载为持久卷：
   - `.agent.properties`（身份信息）
   - `workspace/`（构建工作空间）
   - `logs/`（日志，可选）
4. **Docker-in-Docker** — 如需在容器构建中使用 Docker，需要挂载 Docker socket 或使用 DinD

## 日志位置

| 日志 | 路径 |
|------|------|
| Agent 主日志 | `logs/devopsAgent.log` |
| Daemon 日志 | `logs/devopsDaemon.log` |
| 构建日志 | `logs/{buildId}_{vmSeqId}_agent.log` |

日志保留时长由 `devops.agent.logs.keep.hours` 控制（默认 96 小时）。

## status 命令示例

```bash
$ ./devopsAgent status
[BK-CI] ============================================
[BK-CI] BK-CI Agent 状态
[BK-CI] ============================================
  平台:                    Linux
  工作目录:                 /data/bkci-agent
  服务名:                  devops_agent_abc123
  当前用户:                 root
  安装模式:                 SERVICE
  运行模式:                 root + systemd (系统服务)
  服务状态:                 active
  开机启动:                 enabled
  服务主进程 PID:           1234
  启动时间:                 Thu 2026-04-08 10:00:00 CST
  Daemon PID:             1234 (运行中)
  Agent PID:              5678 (运行中)
  JDK 17:                 正常 ✓
  JDK 8:                  正常 ✓
  worker-agent.jar:       正常 ✓ (45.2 MB)

  健康检查
  --------------------------------------------
  磁盘可用:                50.1 GB ✓
  磁盘可写:                正常 ✓

  网关 (devops.example.com):
    DNS resolve:          10.0.0.1 (1ms) ✓
    TCP connect:          10.0.0.1:443 (3ms) ✓
    TLS handshake:        TLS 1.3 (8ms) ✓
    HTTP GET:             200 OK (25ms) ✓
    Proxy:                直连 (无代理)
```

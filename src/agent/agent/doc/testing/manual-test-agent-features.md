# BK-CI Agent 人工测试文档

## 1. 目的

验证以下本次改动功能在真实环境中的可用性与兼容性：

- CLI 子命令安装/卸载/启动/停止/重装
- Windows service / session / task 三种安装模式
- `reinstall` 内置下载和心跳等待机制
- `status` 健康检查
- Docker / Podman CLI 运行时切换
- Docker 构建容器生命周期
- 镜像调试 `exec` 交互终端
- 旧脚本兼容

## 2. 测试环境矩阵

建议至少覆盖以下环境：

| 平台 | 容器运行时 | 权限 | 重点 |
|------|------------|------|------|
| Linux | docker | root + systemd | install/reinstall/status/docker build |
| Linux | podman | 可运行 podman 的用户 | runtime 切换、容器构建、日志 |
| macOS | docker/podman | 普通用户 / root | install/status/reinstall |
| Windows | docker | 管理员 | service/session/reinstall/status |
| Windows | 无容器运行时 | 管理员 | CLI 基础功能、Windows 模式安装 |

## 3. CLI 基础验证

### 3.1 帮助

执行：

```bash
./devopsAgent -h
```

验证：

- Linux/macOS 不展示 Windows 独有参数：
  - `session`（install 子模式）
  - `task`（install 子模式）
  - `configure-session`
  - `--auto-logon`
- Windows 展示完整 Windows 参数
- `status` 文案包含健康检查说明
- `version` 文案为：
  - `version`
  - `version -f`

### 3.2 版本命令

执行：

```bash
./devopsAgent version
./devopsAgent version -f
```

验证：

- `version` 输出 1 行版本号
- `version -f` 输出 3 行：版本号 / Git Commit / BuildTime
- `fullVersion` 执行应报 unknown command

### 3.3 debug

执行：

```bash
./devopsAgent debug
./devopsAgent debug on
./devopsAgent debug
./devopsAgent debug off
./devopsAgent debug
```

验证：

- `debug on` 创建 `.debug`
- `debug off` 删除 `.debug`
- 提示为“重启后生效”
- 重启 agent 后：
  - 自动升级关闭
  - Docker 容器不自动删除
  - 日志更详细

## 4. Linux / macOS 服务管理

### 4.1 install

执行：

```bash
./devopsAgent install
```

#### Linux root + systemd

验证：

- 生成 `/etc/systemd/system/devops_agent_{id}.service`
- `WorkingDirectory` 为绝对路径
- `systemctl status` 正常
- `runtime/daemon.pid` / `runtime/agent.pid` 正常

#### Linux 非 root

验证：

- 不注册 systemd
- daemon 直接后台启动
- `status` 显示 `non-root + direct`

#### Linux 非 root + user 模式

验证：

- `install user` 注册到 `~/.config/systemd/user/`
- `systemctl --user status devops_agent_{id}` 正常
- linger 自动启用

#### Linux LDAP/域账号 + user 模式

验证（需在 LDAP 用户环境下测试）：

- `install user` 时打印网络账号警告（提示 linger 开机自启可能不可靠）
- 重启机器后确认 Agent 是否自动拉起
- `sudo journalctl -b -u systemd-logind | grep linger` 可见 "Couldn't add lingering user" 错误
- 切换为 `sudo ./devopsAgent install` 后重启可正常自启

#### macOS

验证：

- root 和非 root 均使用 `~/Library/LaunchAgents`（不使用 `/Library/LaunchDaemons`）
- `status` 中 plist 路径正确
- 从旧版 Root LaunchDaemon 升级时，`cleanupLegacyPlist` 自动清理 `/Library/LaunchDaemons` 中的旧 plist

### 4.2 start / stop / uninstall

执行：

```bash
./devopsAgent stop
./devopsAgent start
./devopsAgent uninstall
```

验证：

- `stop` 后 daemon/agent 进程退出
- `start` 后重新拉起
- `uninstall` 后服务/launchd/systemd 定义删除
- `status` 正确反映未注册状态

## 5. Windows 安装模式验证

### 5.1 Service 模式

执行：

```powershell
.\devopsAgent.exe install
.\devopsAgent.exe status
```

验证：

- 创建 Windows 服务 `devops_agent_{id}`
- `status` 中运行模式为 `SERVICE`
- 服务能正常拉起 daemon 和 agent
- `status` 末尾汇总为“正常”

### 5.2 Session 模式

执行：

```powershell
.\devopsAgent.exe install session --user xxx --password yyy
.\devopsAgent.exe status
```

再执行：

```powershell
.\devopsAgent.exe install session --user xxx --password yyy --auto-logon
```

验证：

- LSA Secret 写入成功
- `status` 能显示：
  - Session credentials
  - Session password
  - Auto-logon 状态
- `status` 末尾新增汇总词条：全部正常时显示“正常”，出现失败/告警时显示“异常”
- 当前登录用户 Session 内可以启动 agent
- 开启 auto-logon 后重启机器，状态仍正确
- `configure-session` 完成后的摘要文案会根据系统语言切换中英文，中文 Windows 控制台下不出现明显乱码

### 5.3 Task 模式（已废弃）

执行：

```powershell
.\devopsAgent.exe install task
.\devopsAgent.exe status
```

验证：

- 安装时显示 deprecated 警告
- `schtasks` 创建成功
- `devopsctl.vbs` 被创建
- `status` 中显示 `TASK (legacy)`

### 5.4 uninstall

执行：

```powershell
.\devopsAgent.exe uninstall
```

验证：

- service / task 都能清理
- `.install_type` 清理
- `devopsctl.vbs` 清理
- Session 凭据清理
- Auto-logon 设置清理

## 6. `reinstall` 验证

### 6.1 正常重装

执行：

```bash
./devopsAgent reinstall -y
```

验证流程顺序：

1. 先下载 `agent.zip`
2. 下载成功后才 stop / clean
3. 再解压 / 安装

重点确认：

- 下载失败时，本地文件不被删除
- 保留文件：
  - `.agent.properties`
  - `.install_type`
  - `.cert`
  - `.debug`
  - `workspace`
  - `devopsAgent[.exe]`

### 6.2 心跳等待

前置：agent 正常运行，刚刚发送过心跳

执行：

```bash
./devopsAgent reinstall -y
```

验证：

- 会先等待约 55 秒
- 等待结束后再尝试下载 `agent.zip`
- 不再出现“Agent already installed. Please obtain the install url again”

### 6.3 Windows 文件锁

前置：Windows 上 agent 正在运行

执行：

```powershell
.\devopsAgent.exe reinstall -y
```

验证：

- `devopsAgent.exe` 会被 rename 为 `.bak`
- 新 `devopsAgent.exe` 能正常解压
- 不出现 `Access denied`
- `.bak` 最终尽量清理成功

## 7. `status` 健康检查

执行：

```bash
./devopsAgent status
```

### 7.1 正常环境

验证输出包含：

- 磁盘空间
- 磁盘可写
- Gateway DNS resolve
- TCP connect
- TLS handshake（https）
- HTTP GET
- Proxy
- Cert (.cert)

### 7.2 故障定位验证

人工制造以下场景并观察 `status`：

#### DNS 故障

- 配置错误域名
- 预期：失败在 `DNS resolve`

#### TCP 故障

- 正确域名但端口不可达
- 预期：失败在 `TCP connect`

#### TLS 证书错误

- https 网关 + 缺少 `.cert`
- 预期：失败在 `TLS handshake`

#### HTTP 认证失败

- 错误 secret key
- 预期：HTTP GET 返回 401/403，并显示状态码

### 7.3 代理场景

配置 `HTTP_PROXY` / `HTTPS_PROXY` / `NO_PROXY` 后执行 `status`

验证：

- 输出中能显示代理地址或 `direct`
- 实际请求路径与 agent 真实行为一致

## 8. Docker / Podman 构建验证

### 8.1 Docker 默认运行时

前置：

```bash
unset DEVOPS_AGENT_CONTAINER_RUNTIME
```

执行 Docker 构建任务，验证：

- 实际运行的是 `docker pull/create/start/wait/logs/rm`
- Agent 日志中能看到完整命令
- 构建日志中能看到关键命令/输出
- 构建成功与失败场景都能正常上报

### 8.2 Podman 运行时

前置：

```bash
export DEVOPS_AGENT_CONTAINER_RUNTIME=podman
```

执行同样构建任务，验证：

- 实际运行的是 `podman`
- 拉镜像 / 起容器 / 等待 / 日志 / 删除 都正常
- `status` / logs 无异常

### 8.3 自定义 DockerOptions

重点验证：

- `Volumes`
- `Mounts`
- `Privileged`
- `Network`
- `User`
- `Gpus`

#### Windows 特别验证

重点验证带盘符的 volume：

- `C:\data:/data`
- `D:\cache:/cache:ro`

确保不会因为 `:` 分割错误导致命令拼接异常。

## 9. 镜像调试 `exec`

### 9.1 创建调试容器

验证：

- 容器能正常拉起
- 创建命令、stdout、stderr 被写入 Agent 日志
- 返回调试 URL 正常

### 9.2 WebSocket shell

进入调试终端，验证：

- 能进入 shell
- 输入输出正常
- 大量输出不丢失
- 中文/UTF-8 正常

### 9.3 resize

调整前端终端大小，验证：

- `resize_exec` 生效
- 容器内 TTY 行列变化正常

### 9.4 多会话限制

同一个容器开第二个 session，验证：

- 第二个连接被拒绝
- 返回提示与预期一致

### 9.5 Podman exec

前置：

```bash
export DEVOPS_AGENT_CONTAINER_RUNTIME=podman
```

验证：

- `create_exec` / `start_exec` / `resize_exec` 仍然可用
- `podman exec -it` 行为正常
- 若运行环境不支持，错误信息足够清晰

## 10. 旧脚本兼容验证

### Linux / macOS

验证：

- `scripts/linux/install.sh`
- `scripts/linux/start.sh`
- `scripts/linux/stop.sh`
- `scripts/linux/uninstall.sh`
- `scripts/macos/*`

### Windows

验证：

- `scripts/windows/install.bat`
- `scripts/windows/start.bat`
- `scripts/windows/stop.bat`
- `scripts/windows/uninstall.bat`
- `scripts/windows/install_schtasks_for_ui.bat`
- `scripts/windows/devopsctl.vbs`

确认：

- 在新 agent 下仍能正常操作
- `stop.bat` 不再死循环
- 旧脚本不会破坏新模式兼容性

## 11. 建议重点回归清单

每次发版前至少手测这几项：

1. Windows `install session`
2. Windows `reinstall`
3. Linux root `install`（systemd 路径必须绝对）
4. `status` 故障定位（DNS/TCP/TLS/HTTP）
5. `DEVOPS_AGENT_CONTAINER_RUNTIME=podman`
6. Docker/Podman 构建日志是否打印完整命令
7. 镜像调试 `exec` + resize
8. Windows 带盘符 volume 参数

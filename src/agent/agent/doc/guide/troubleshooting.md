# 故障排查与状态检查

本文档介绍如何使用 BK-CI Agent 的内置诊断工具排查常见问题。

## status 命令

`devopsAgent status` 是排查问题的首选工具，它提供一站式状态检查：

```bash
./devopsAgent status          # Linux / macOS
.\devopsAgent.exe status      # Windows
```

### 输出结构

status 命令的输出分为四个部分：

#### 1. 基本信息

```
  平台:              Linux
  工作目录:           /data/bkci-agent
  服务名:            devops_agent_abc123
  当前用户:           root
  安装模式:           SERVICE
  运行模式:           root + systemd (系统服务)
```

- **安装模式**：当前 Agent 的安装方式（SERVICE/USER/DIRECT/LOGIN/BACKGROUND/SESSION）
- **运行模式**：对安装模式的详细解释

#### 2. 服务与进程状态

```
  服务状态:           active
  开机启动:           enabled
  Daemon PID:        1234 (运行中)
  Agent PID:         5678 (运行中)
```

各状态含义：

| 状态 | 含义 | 处理方式 |
|------|------|---------|
| `active` / `运行中` | 正常 | 无需操作 |
| `inactive` / `未运行` | 进程未启动 | 执行 `start` |
| `已退出` | 进程 PID 存在但进程已不存在 | 执行 `start` 重启 |
| `not registered` / `未注册` | 服务未注册 | 执行 `install` |

#### 3. 依赖检查

```
  JDK 17:            正常 ✓
  JDK 8:             正常 ✓
  worker-agent.jar:  正常 ✓ (45.2 MB)
```

各状态含义：

| 状态 | 含义 | 处理方式 |
|------|------|---------|
| `正常 ✓` | 文件存在且有效 | 无需操作 |
| `缺失 ✗` | 文件不存在 | 执行 `repair` 或 `reinstall` |
| `空文件 ✗` | 文件存在但大小为 0 | 执行 `repair` 或 `reinstall` |
| `非目录 ✗` | 路径存在但不是目录 | 手动删除后 `repair` |

#### 4. 健康检查

```
  健康检查
  --------------------------------------------
  磁盘可用:           50.1 GB ✓
  磁盘可写:           正常 ✓

  网关 (devops.example.com):
    DNS resolve:     10.0.0.1 (1ms) ✓
    TCP connect:     10.0.0.1:443 (3ms) ✓
    TLS handshake:   TLS 1.3 (8ms) ✓
    HTTP GET:        200 OK (25ms) ✓
    Proxy:           直连 (无代理)

  证书 (.cert):      已加载 ✓
```

健康检查会逐步测试网络连通性，在第一个失败的步骤停止，帮助快速定位问题层级。

### 健康检查详解

| 检查项 | 检查内容 | 常见失败原因 |
|--------|---------|------------|
| 磁盘可用 | 工作目录所在分区的可用空间 | 构建产物或日志占满磁盘 |
| 磁盘可写 | 尝试在工作目录写入测试文件 | 文件系统只读、权限问题 |
| DNS resolve | 解析网关域名 | DNS 服务器不可用、域名不存在 |
| TCP connect | TCP 连接到网关 IP:端口 | 防火墙规则、网关服务宕机 |
| TLS handshake | TLS 握手协商 | 证书问题、TLS 版本不兼容 |
| HTTP GET | 发送 HTTP 请求到网关 API | 网关应用层问题、认证失败 |
| Proxy | 显示实际使用的代理 | 代理配置错误 |
| 证书 | 加载 `.cert` 文件 | PEM 格式错误 |

## 常见故障场景

### Agent 在平台上显示离线

**症状**：BK-CI 平台上 Agent 显示离线状态。

**排查步骤**：

1. 检查进程是否运行：
   ```bash
   ./devopsAgent status
   ```

2. 如果进程未运行，查看日志：
   ```bash
   tail -100 logs/devopsAgent.log
   ```

3. 如果进程在运行但平台显示离线，检查网络：
   ```bash
   ./devopsAgent status   # 查看健康检查部分
   ```

4. 常见原因及解决：

| 原因 | 现象 | 解决方式 |
|------|------|---------|
| Agent 进程未启动 | PID 状态为"未运行" | `./devopsAgent start` |
| 网络不通 | DNS/TCP/TLS 检查失败 | 检查防火墙、DNS、代理设置 |
| 网关地址错误 | HTTP GET 失败 | 检查 `.agent.properties` 中的 `landun.gateway` |
| 密钥错误 | HTTP 返回 401/403 | 检查 `.agent.properties` 中的 `secret.key` |
| 时钟偏差 | TLS 握手失败 | 同步系统时间（NTP） |

### 构建任务卡住不结束

**症状**：流水线构建一直处于"执行中"状态。

**排查步骤**：

1. 检查 Worker 进程是否存在：
   ```bash
   ps aux | grep worker-agent.jar
   ```

2. 查看构建日志：
   ```bash
   ls -la logs/ | grep <buildId>
   tail -100 logs/<buildId>_<vmSeqId>_agent.log
   ```

3. 检查磁盘空间（磁盘满会导致 Worker 卡住）：
   ```bash
   ./devopsAgent status
   ```

4. 如需强制终止：
   ```bash
   # 在 BK-CI 平台上取消构建
   # 或手动终止 Worker 进程
   kill <worker_pid>
   ```

### 构建失败提示 JDK 缺失

**症状**：构建错误信息提示找不到 Java 或 JDK。

**排查步骤**：

1. 检查 JDK 目录状态：
   ```bash
   ./devopsAgent status   # 查看 JDK 17 和 JDK 8 状态
   ```

2. 如果显示"缺失"，修复 JDK：
   ```bash
   ./devopsAgent repair
   ```

3. 如果 `repair` 无效（本地没有 JDK zip 包），完全重装：
   ```bash
   ./devopsAgent reinstall
   ```

### Worker 启动失败

**症状**：构建快速失败，日志提示 Worker 进程启动异常。

**排查步骤**：

1. 检查 `worker-agent.jar` 是否存在：
   ```bash
   ./devopsAgent status   # 查看 worker-agent.jar 状态
   ```

2. 检查 `devops.slave.user` 配置的用户是否存在且有权限
3. 检查 JDK 是否可执行：
   ```bash
   jdk17/bin/java -version    # Linux
   jdk17/Contents/Home/bin/java -version    # macOS
   ```

### 网络相关问题

**症状**：Agent 在线但构建中网络操作失败。

`status` 命令的网络检查会模拟 Agent 的实际连接方式（包含代理、证书、认证头），输出类似 `curl -v` 的分步骤信息：

```
  网关 (devops.example.com):
    DNS resolve:     FAIL: lookup devops.example.com: no such host ✗
```

根据失败的步骤确定问题层级：

| 失败步骤 | 问题层级 | 排查方向 |
|---------|---------|---------|
| DNS resolve | DNS 解析 | 检查 `/etc/resolv.conf`、DNS 服务器 |
| TCP connect | 网络连通性 | 检查防火墙、路由、端口 |
| TLS handshake | 加密通信 | 检查证书、TLS 版本、`.cert` 文件 |
| HTTP GET | 应用层 | 检查网关服务状态、认证信息 |

### 磁盘空间不足

**症状**：构建失败，日志提示写入失败或"no space left on device"。

**排查步骤**：

1. 确认磁盘状态：
   ```bash
   ./devopsAgent status   # 查看磁盘可用空间
   df -h                  # 查看整体磁盘使用情况
   ```

2. 清理空间：
   ```bash
   # 清理构建工作空间（选择性删除不需要的流水线目录）
   du -sh workspace/*

   # 手动清理日志（Agent 也会自动清理）
   find logs/ -name "*.log" -mtime +3 -delete
   ```

3. 调整日志保留时间：
   在 `.agent.properties` 中减少 `devops.agent.logs.keep.hours`。

## 日志位置

| 日志 | 路径 | 内容 |
|------|------|------|
| Agent 主日志 | `logs/devopsAgent.log` | Agent 主循环、心跳、升级 |
| Daemon 日志 | `logs/devopsDaemon.log` | Daemon 启停记录 |
| 构建日志 | `logs/{buildId}_{vmSeqId}_agent.log` | 单次构建的 Worker 输出 |
| Docker 构建日志 | `logs/docker/{buildId}/` | Docker 容器构建日志 |

### 开启详细日志

```bash
./devopsAgent debug on
./devopsAgent start   # 重启生效
```

开启后日志包含更多细节（HTTP 请求/响应、环境变量变化等），排查完毕后建议关闭：

```bash
./devopsAgent debug off
./devopsAgent start   # 重启生效
```

注意：调试模式下 Agent 不会接受自动升级。

## 进程与服务管理命令速查

### Linux

```bash
# systemd 服务状态
sudo systemctl status devops_agent_{id}
sudo journalctl -u devops_agent_{id} -f

# 用户级 systemd
systemctl --user status devops_agent_{id}
```

### macOS

```bash
# launchd 状态
launchctl print gui/$(id -u)/devops_agent_{id}

# 查看 plist
cat ~/Library/LaunchAgents/devops_agent_{id}.plist
```

### Windows

```powershell
# 服务状态
sc.exe query devops_agent_{id}

# 事件日志
Get-EventLog -LogName System -Source "Service Control Manager" | 
  Where-Object { $_.Message -like "*devops_agent*" } | 
  Select-Object -First 10
```

## 诊断信息收集

如果需要向技术支持提供诊断信息，请收集以下内容：

```bash
# 1. status 完整输出
./devopsAgent status > agent_status.txt 2>&1

# 2. 最近的日志
tail -500 logs/devopsAgent.log > agent_log.txt

# 3. 配置信息（注意隐去 secret.key）
grep -v secret .agent.properties > agent_config.txt

# 4. 系统信息
uname -a > system_info.txt
df -h >> system_info.txt
```

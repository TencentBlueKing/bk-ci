# 升级与重装机制

本文档介绍 BK-CI Agent 的自动升级流程和手动重装机制。

## 自动升级

Agent 具备无感知热升级能力，由服务端统一控制。

### 升级流程

```
服务端标记需要升级
       │
       ▼
  Agent 在 Ask 轮询中检测到升级指令
       │
       ├── 有构建正在运行 → 推迟升级（上报 skip）
       │
       ▼
  锁定构建槽位（阻止新构建）
       │
       ▼
  下载更新文件到升级目录
  (upgrader、daemon、agent、worker、JDK、docker_init)
       │
       ▼
  MD5/版本比对，确定哪些文件有变化
       │
       ├── Worker/JDK/docker_init 变化 → 直接替换
       │
       ├── Agent/Daemon 变化 → 启动 upgrader 进程
       │   │
       │   ▼
       │   upgrader 进程:
       │     1. 替换 devopsAgent 和 devopsDaemon 二进制
       │     2. 终止旧进程
       │     3. 通过 systemd/launchd/sc.exe 重启服务
       │
       └── 无变化 → 解锁槽位，继续正常运行
```

### 可升级的组件

| 组件 | 升级方式 | 说明 |
|------|---------|------|
| `devopsAgent` | upgrader 替换 + 重启 | Agent 主程序 |
| `devopsDaemon` | upgrader 替换 + 重启 | 守护进程 |
| `worker-agent.jar` | 直接覆盖 | 构建 Worker，下次构建自动使用新版 |
| JDK 17 | 解压到带时间戳目录 → 替换 | 避免覆盖正在使用的 JDK |
| `docker_init.sh` | 直接覆盖 | Docker 容器入口脚本 |

### 不会自动升级的内容

| 内容 | 原因 | 更新方式 |
|------|------|---------|
| 安装脚本（`install.sh` 等） | 仅首次安装时使用 | 使用 `reinstall` 命令 |
| 系统服务注册（systemd unit 等） | 格式可能不同但功能等价 | 使用 `reinstall` 或 `install` 命令 |
| `.agent.properties` 配置 | 身份信息不可变 | 心跳同步或手动修改 |

### 升级的前提条件

- 当前没有正在运行的构建任务
- 没有其他升级正在进行

### Daemon 重启机制

升级完成后，Daemon 负责拉起新版 Agent。不同平台的检测策略不同：

| 平台 | 检测方式 | 典型重启耗时 |
|------|---------|------------|
| Linux/macOS | 5 秒 ticker + flock 文件锁，发现 Agent 锁可获取即重新拉起 | ~5s |
| Windows | 3s base delay + 每 500ms 轮询 total-lock（TryLock），锁可用即重启，30s 兜底超时 | ~3-5s（无升级）/ upgrader 完成后立即（升级中） |

Windows 上的 `waitBeforeRestart()` 机制：
1. **3 秒基础延迟** — 防止 Agent 崩溃时快速重启风暴
2. **500ms 轮询 total-lock** — upgrader 持有此锁期间表示正在替换文件，锁释放意味着升级完成
3. **30 秒兜底超时** — 如果 upgrader 异常卡住，最多等 30 秒后仍然尝试重启

### 升级失败的处理

- 下载失败 → 保留当前版本，下次轮询重试
- 升级中断 → Daemon 检测到 Agent 异常退出并重启
- 版本回退 → 服务端可以下发旧版本实现回退

## 手动重装 (reinstall)

当需要完全重装 Agent（修复损坏的文件、更新所有组件和脚本）时，使用 `reinstall` 命令。

### 使用方式

```bash
# 交互式（需要确认）
./devopsAgent reinstall

# 跳过确认
./devopsAgent reinstall -y
```

### 重装流程

```
用户执行 reinstall
       │
       ▼
  显示操作说明，等待用户确认 (或 -y 跳过)
       │
       ▼
  步骤 1: 等待心跳过期
  (服务端需要检测到 Agent 离线后才允许重新下载)
       │
       ▼
  步骤 2: 从服务端下载 agent.zip
  (使用 .agent.properties 中的凭据，自动检测 CPU 架构)
       │
       ▼
  步骤 3: 验证下载文件
  (检查 ZIP 魔术字节 PK\x03\x04)
       │
       ▼
  步骤 4: 停止 Agent 服务
       │
       ▼
  步骤 5: 清理文件
  (删除除保留文件外的所有内容)
       │
       ▼
  步骤 6: 解压 agent.zip
  (Windows: 重命名运行中的 exe 避免文件锁)
       │
       ▼
  步骤 7: 执行安装
  (调用 install 命令注册服务并启动)
```

### 保留文件清单

重装过程中以下文件不会被删除：

| 文件 | 原因 |
|------|------|
| `.agent.properties` | Agent 身份信息，删除后无法重新连接服务端 |
| `.install_type` | 安装模式记录，reinstall 后自动使用相同模式 |
| `.cert` | 自定义 TLS 证书 |
| `.env` | 环境变量快照（Linux/macOS） |
| `.path` | PATH 快照（Linux/macOS） |
| `workspace/` | 构建工作空间，包含用户代码和产物 |
| Agent 二进制自身 | 重装命令正在运行，不能删除自己 |

### 架构检测

`reinstall` 会自动检测当前机器的 CPU 架构，在下载 URL 中附加 `?arch=` 参数：

| `runtime.GOARCH` | 后端参数 | 下载的包 |
|-------------------|---------|---------|
| `arm64` | `?arch=arm64` | ARM64 专用包 |
| `mips64` / `mips64le` | `?arch=mips64` | MIPS64 专用包 |
| 其他（amd64, 386, loong64） | 不附加参数 | 默认 x86_64 包 |

### Windows 文件锁处理

Windows 不允许删除/覆盖正在运行的 `.exe` 文件。`reinstall` 会自动处理：
1. 解压前将 `devopsAgent.exe` 重命名为 `devopsAgent.exe.bak`
2. 解压新文件
3. 清理 `.bak` 文件

### 与自动升级的区别

| 特性 | 自动升级 | reinstall |
|------|---------|-----------|
| 触发方式 | 服务端控制 | 用户手动 |
| 范围 | 仅更新有变化的组件 | 全部重新下载替换 |
| 脚本更新 | 不更新 | 全部更新 |
| 服务注册 | 不更新 | 重新注册 |
| 中断构建 | 等待构建完成 | 立即停止 |
| 适用场景 | 日常升级 | 修复损坏、大版本升级 |

## 修复命令 (repair)

对于轻量级修复（如 JDK 被误删），可以使用 `repair` 命令：

```bash
./devopsAgent repair
```

`repair` 的操作：
1. 停止 Agent
2. 重新解压 JDK 和依赖（从已有的 zip 包）
3. 重新启动 Agent

与 `reinstall` 的区别：`repair` 不会从服务端重新下载，只是重新解压本地已有的安装包。

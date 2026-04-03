# 构建系统与工作空间

本文档介绍 BK-CI Agent 的构建任务执行流程、Worker 进程管理、JDK 管理、工作空间使用和资源占用。

## 构建流程

Agent 接收到服务端的构建任务后，执行以下流程：

```
服务端下发构建任务
       │
       ▼
  申请构建槽位
  (检查并发数限制)
       │
       ├── 槽位已满 → 告知服务端暂不接受新任务
       │
       ▼
  准备构建环境
  (创建临时目录、生成启动脚本)
       │
       ▼
  启动 Worker 进程
  (java -jar worker-agent.jar)
       │
       ▼
  Worker 执行构建逻辑
  (代码检出、编译、测试、部署等)
       │
       ▼
  Worker 退出
       │
       ▼
  上报构建结果给服务端
       │
       ▼
  清理临时文件，释放槽位
```

## Worker 进程

### 启动方式

Agent 通过生成 shell 脚本（Linux/macOS）或直接调用（Windows）来启动 Worker 进程。

#### Linux / macOS

使用两层脚本嵌套确保加载用户 profile：

```bash
# 外层 prepare 脚本
#!/bin/bash
exec /bin/bash -l start_script.sh

# 内层 start 脚本
#!/bin/bash
cd <工作目录>
<java路径> -Xmx2g -Djava.io.tmpdir=<临时目录> -jar worker-agent.jar <构建信息>
```

`-l` 标志使 shell 以 login shell 方式运行，加载 `~/.bash_profile` 等初始化文件。

#### Windows

直接调用 Java 进程启动 Worker：

```
<java路径> -Xmx2g -Djava.io.tmpdir=<临时目录> -jar worker-agent.jar <构建信息>
```

### JVM 参数

| 参数 | 说明 |
|------|------|
| `-Xmx2g` | Worker 进程最大堆内存 2GB |
| `-Djava.io.tmpdir=<tmpDir>` | Java 临时文件目录，指向 `build_tmp/` 下的构建专属目录 |
| `-Dbuild.type=AGENT` | 标识构建类型为 Agent 模式 |
| `-DAGENT_LOG_PREFIX=<buildId>` | 日志前缀，用于区分不同构建任务的日志 |
| `-Ddevops.slave.agent.start.file=<path>` | 启动脚本路径（构建结束后清理） |
| `-Ddevops.agent.error.file=<path>` | 错误信息文件路径 |

### 异常处理

构建开始前，Agent 会预先写入一条"构建进程被杀死"的错误消息到 `build_tmp/{buildId}_{vmSeqId}_build_msg.log`。如果 Worker 正常结束，会清除此文件。如果进程被操作系统或人为终止，Agent 可以通过读取此文件获取最后的错误状态并上报。

## JDK 管理

Agent 自带 JDK 运行时，不依赖系统安装的 Java：

| JDK 版本 | 目录 | 优先级 |
|----------|------|--------|
| JDK 17 | `jdk17/` | 首选，新构建任务默认使用 |
| JDK 8 | `jdk/` | 兜底，JDK 17 不可用时回退使用 |

**版本检测逻辑**：Agent 通过 `java -version` / `java --version` 检测 JDK 版本，结果缓存并在心跳中上报服务端。版本检测使用文件修改时间判断是否需要重新检测，避免频繁执行命令。

**自动升级**：服务端可以下发新版 JDK，Agent 会自动下载并解压到 `jdk17-{timestamp}` 目录，升级完成后替换为 `jdk17`。

**macOS 路径差异**：macOS 的 JDK 目录结构包含额外的 `Contents/Home/` 层级，Agent 会自动处理。

## 工作空间

### workspace 目录

构建任务的代码检出和执行都在 `workspace/` 目录下进行。

**重要特性：**
- `workspace/` 目录下的内容**不会被自动清理**
- 每个流水线通常在 `workspace/{pipelineId}/` 下有独立的子目录
- 磁盘占用会持续增长，需要用户自行管理

**建议：**
- 在流水线中添加清理步骤（如 `git clean`）
- 定期手动清理不再使用的流水线目录
- 监控磁盘空间，使用 `devopsAgent status` 查看可用空间

### build_tmp 目录

构建临时文件存放目录，包含：
- JVM 临时文件
- 构建错误消息文件（`{buildId}_{vmSeqId}_build_msg.log`）
- 启动脚本文件（`devops_agent_start_*.sh`）

**自动清理**：Agent 会自动清理 7 天以上的临时文件。

### logs 目录

Agent 运行日志、构建日志存放目录。

**自动清理**：由 `devops.agent.logs.keep.hours` 配置控制，默认保留 96 小时（4 天）。清理任务每 2 小时执行一次。

## 资源占用

### 内存

| 组件 | 内存占用 |
|------|---------|
| devopsAgent 进程 | ~30-50 MB |
| devopsDaemon 进程 | ~10-20 MB |
| 每个 Worker 进程 | 最大 2 GB（`-Xmx2g`） |
| Docker 构建 Worker | 取决于容器配置 |

**最坏情况估算**：Agent 自身 ~70MB + 并发数 × 2GB。例如 `parallel.task.count=4` 时，峰值内存约 8.07 GB。

### 磁盘

| 项目 | 大小 | 是否自动清理 |
|------|------|------------|
| Agent 二进制 + JDK + Worker | ~300-500 MB | 否（升级时替换） |
| 日志文件 | 随运行时间增长 | 是（默认 4 天） |
| 构建临时文件 | 随构建量增长 | 是（7 天） |
| workspace 构建产物 | 持续增长 | **否** |

**磁盘满的影响**：磁盘满会导致构建失败、日志无法写入。`status` 命令会检测磁盘空间并在低于阈值时告警。

### CPU

Agent 自身的 CPU 占用很低（主要是 5 秒一次的 HTTP 轮询和 Windows 3 秒一次的注册表轮询）。CPU 消耗主要来自构建任务本身。

### 网络

Agent 每 5 秒向服务端发送一次 Ask 请求（包含心跳信息），数据量很小（通常几 KB）。网络带宽消耗主要来自：
- 构建过程中的代码拉取
- 产物上传下载
- JDK/Worker 升级下载

## 并发控制

| 类型 | 配置项 | 默认值 | 说明 |
|------|--------|--------|------|
| 普通构建 | `devops.parallel.task.count` | 4 | 直接在宿主机运行的构建 |
| Docker 构建 | `devops.docker.parallel.task.count` | 4 | 在容器中运行的构建 |

两类构建独立计数，互不影响。当某类构建达到上限时，Agent 在 Ask 请求中会告知服务端暂不接受该类型的新任务。

升级期间，Agent 会锁定所有构建槽位，等待运行中的任务完成后再执行升级。如果有正在运行的构建，升级会被推迟到构建完成。

## 定时清理任务

Agent 后台运行一个 Cron 任务，每 2 小时执行一次：

| 清理项 | 规则 |
|--------|------|
| JVM crash dump 文件 (`hs_err_pid*`) | 清理工作目录下的 dump 文件 |
| 日志文件 (`.log`) | 清理超过 `logs.keep.hours` 的日志 |
| Docker 构建日志 | 按文件夹修改时间清理 |
| 调试容器 | 清理超时的 Docker 调试容器 |

临时文件的清理（`build_tmp/` 下超过 7 天的文件）在每次构建结束后触发。

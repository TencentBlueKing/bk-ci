# BK-CI Agent SDK for Rust

面向受管 Agent 的 Rust SDK。首个默认程序用于 aihub 子进程接入，继续使用现有第三方 Agent 后台协议和 Java worker。

参考同目录 Node.js SDK 的 AgentLoop / AgentApi / DefaultBuildRunner，按当前后台和 Go Agent 的行为实现协议；没有依赖 Go、Node.js 进程或 FFI。

## 组件

| Crate | 用途 |
| --- | --- |
| `bk-ci-agent-sdk` | HTTP 客户端、协议类型、任务主循环、动态配置、并发与任务状态、执行器接口 |
| `bk-ci-agent-worker` | 官方 Java worker 默认执行器 |
| `bk-ci-agent-managed` | 标准输入输出受管程序，供 aihub 等宿主调用 |

SDK 是库：不安装全局日志、不切换进程 cwd、不接管信号、不创建自己的 Tokio runtime、不退出宿主进程。宿主提供配置、metadata、Executor 和停止信号。

第一版支持普通 Java worker 构建；默认执行器不支持 Docker。升级、监控采集、JDK 下载/安装、MCP、镜像调试、流水线脚本执行均不装配到默认程序。自定义 Executor 可以实现 Docker 执行，SDK 据其能力声明生成 askEnable。

业务节点可用状态、权限、资源与 Agent 绑定由 environment/dispatch 决定；SDK 不查询数字分身后台。每个子进程对应一个已分配身份的 Agent。凭证和 worker/JDK 版本由 aihub 提供。

## 基础组件约定

通用能力优先采用维护中的 Cargo 库，业务层只编写 BK-CI 协议适配和任务状态逻辑，不再自建日志、配置或参数解析框架。依赖集中在 workspace 声明，版本由 Cargo.lock 固定；新增依赖需确认适用范围、维护状态、许可证和跨平台兼容性。

| 能力 | 选型 | 使用边界 |
| --- | --- | --- |
| 日志事件 | `tracing` | SDK/worker 发出事件，不安装全局 subscriber |
| 日志输出/过滤 | `tracing-subscriber` | 仅 managed 程序初始化，支持 JSON/text、过滤和 stderr 输出 |
| 命令行 | `clap` derive | 参数、帮助、版本、环境变量优先级和错误提示 |
| 配置文件与合并 | `config` + `serde` | managed 加载 JSON/TOML 默认值；合并后保持 RPC 字段严格类型校验 |
| 异步与停止信号 | `tokio` + `tokio-util` | 执行任务、子进程、CancellationToken |
| 输入分帧 | `tokio-util::codec::LinesCodec` | UTF-8、换行和长度限制；保留薄的阻塞 stdin 桥接以保证退出行为 |
| HTTP/序列化/错误 | `reqwest`、`serde_json`、`thiserror` | 不另建通用框架 |
| 临时执行目录/编码 | `tempfile`、`base64` | 复用标准实现 |

兼容性例外：worker 的 Java Properties 写入暂保留一个小型 UTF-16 转义函数。[java-properties 2.0.0 的 writer](https://docs.rs/crate/java-properties/2.0.0/source/src/lib.rs) 使用 Windows-1252，并将非 BMP 字符写成单个 Unicode 转义；真实 JVM 回归测试无法通过。现有实现覆盖中文、补充字符、欧元符号、控制字符和属性分隔符，待有兼容的库后再替换。

## 构建和测试

工具链要求 Rust 1.97+，已在 Windows x64、Rust 1.97.1 验证。Cargo.lock 纳入版本管理。

```powershell
cargo build --workspace --locked
cargo test --workspace --locked
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo fmt --all --check
```

真实 JVM 测试另行开启，会在临时目录编译测试 JAR，并使用模拟 HTTP 后台，不连接生产服务：

```powershell
$env:BK_CI_TEST_JDK = 'D:/jdks/your-jdk17'
cargo test --workspace --locked -- --ignored
```

覆盖：命令行与配置错误、JSON/TOML 配置合并与严格类型、stderr 日志隔离、分帧边界、协议字段/鉴权、动态并发、同流水线多 Job、执行次数、重复任务、启动重试取消、领取中停止、完成上报失败、删除状态、JSON-RPC、stdin 保持打开时退出、Java 参数/环境/配置转义、错误标记和进程取消。

测试 JAR 验证的是启动契约；正式 worker-agent.jar 和实际 BK-CI 部署仍需接入联调。Linux/macOS 路径有跨平台实现，尚未在这些系统实测。

## Rust 嵌入

构造 AgentConfig、AgentMetadata、Backend、Executor 后运行：

```rust,ignore
let shutdown = Shutdown::new();
let agent = Agent::new(
    config,
    metadata,
    Arc::new(HttpBackend::new()?),
    Arc::new(JavaWorker::new(worker_options)?),
    RuntimeOptions::default(),
)?;
let events = agent.subscribe(); // 可选，必须在 run 前订阅
agent.run(shutdown.clone()).await?;
```

可编译示例：

```powershell
cargo run -p bk-ci-agent-worker --example embedded -- examples/initialize.json
```

先编辑示例配置，将身份、凭证、网关、Java/JAR 路径和版本替换为宿主实际提供的值。

自定义执行器实现 `Executor::execute(BuildInfo, ExecutionContext)`，返回 BuildOutcome。SDK 统一维护任务表并调用 workerBuildFinish，执行器不要重复上报。执行器需响应 context.cancellation，正常返回前回收自己启动的直接子进程。

可直接使用 HttpBackend 的 startup / ask / finish 方法自行编排，或实现 Backend 注入测试/其他传输。自定义 Backend 必须提供有界的请求超时；默认 HttpBackend 使用 requestTimeoutMs。

## aihub 子进程接入

构建后直接启动 `target/debug/bk-ci-agent-managed.exe`（其他系统没有 .exe）。程序以前台方式运行。运行配置默认经 stdin 传入，可通过 --config 提供文件默认值；命令行仅接收文件路径和日志选项，不接收 Agent 凭证。

- JSON-RPC 2.0，一行一个 UTF-8 JSON 对象；单帧上限 1 MiB，不支持批量请求。
- stdout 只输出协议消息；Agent 日志写入 stderr，worker stdout/stderr 写入每次执行的日志文件。
- 请求 id 支持字符串、数字、null；不带 id 的合法通知不返回 response。
- 父进程必须持续读取 stdout，保持 stdin 打开；stdin EOF 请求 drain。
- 父进程管理自身退出后的进程树、硬退出期限、分发和升级。

### 日志与可选配置文件

```powershell
target/debug/bk-ci-agent-managed.exe --help
target/debug/bk-ci-agent-managed.exe --log-format json --log-filter "info,bk_ci_agent_worker=debug"
target/debug/bk-ci-agent-managed.exe --config examples/managed-defaults.toml
```

日志默认以 JSON 输出到 stderr。可选 `--log-format text`，过滤规则优先级为 `--log-filter` > `RUST_LOG` > `info`，语法采用 tracing EnvFilter。aihub 应持续消费 stdout 和 stderr；日志采集、保留与轮转由宿主负责。SDK 嵌入场景由调用方安装自己的 tracing subscriber。

日志只包含生命周期、任务标识、执行状态等字段，不输出完整配置、环境变量、构建载荷或 worker 错误正文。诊断正文通过 AgentEvent/JSON-RPC 事件提供给宿主。Worker 自身的 stdout/stderr 仍保存在每次执行目录。

`--config` 可选，格式为 JSON/TOML，结构与 initialize.params 相同，允许只提供部分默认值。配置文件在进程启动时读取一次，收到 `agent.initialize` 才启动 Agent。字段优先级为初始化参数 > 文件默认值 > Rust 类型默认值；嵌套对象逐字段合并，类型不匹配会拒绝初始化。未指定文件时，继续由 aihub 通过 stdin 提供全部必要参数。配置文件不自动发现、不热加载，也不从环境变量读取 Agent 凭证。运行后的后台心跳配置更新规则保持一致。

### 初始化

把 `examples/initialize.json` 的内容作为 params：

```json
{"jsonrpc":"2.0","id":1,"method":"agent.initialize","params":{"protocolVersion":1,"config":{},"metadata":{},"worker":{}}}
```

上面仅展示消息外形，完整必需字段见示例文件。空配置会被拒绝。

初始化确认：

```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":1,"initialized":true}}
```

确认表示配置检查完成。启动登记成功后另外发出：

```json
{"jsonrpc":"2.0","method":"agent.ready","params":{"type":"ready"}}
```

初始化仅允许一次。网络不通时会重试启动登记，期间可以发送 shutdown。

配置分组：

| 分组 | 主要内容 |
| --- | --- |
| config | gateway、fileGateway、projectId、agentId、secretKey、初始并发、language、请求超时、环境变量 |
| metadata | 主机与版本信息、agentInstallPath、startedUser；不由 SDK 探测 JDK |
| worker | javaExecutable、workerJar、dataDir（必须为绝对路径），maxHeap、环境变量 |
| runtime | 轮询/重试间隔、成功上报延迟、完成上报次数、近期任务去重容量 |

agentInstallPath 是心跳元数据，dataDir 是该 Agent 的私有实例目录。aihub 应为不同 Agent 分配不同目录，并使用受限目录权限。secretKey 只使用 Agent 凭证；本 SDK 不执行 Node.js 示例中的 token/deviceId/userId 注册流程。

### 停止

```json
{"jsonrpc":"2.0","id":2,"method":"agent.shutdown","params":{"mode":"drain"}}
```

drain 停止接单、继续运行中任务心跳，等待已领取任务及完成上报结束。若 stop 发生在领任务请求途中，返回的已领取任务仍会执行和上报。

```json
{"jsonrpc":"2.0","id":3,"method":"agent.shutdown","params":{"mode":"terminate"}}
```

terminate 同时向执行器发取消通知。JavaWorker 会终止并等待直接 Java 子进程，SDK 随后上报执行失败。完整进程树（包括构建脚本启动的后代）仍由 aihub 管理。当前 HTTP 请求会在完成/请求超时后收尾，不因 drain 直接丢弃。

程序最后发出 agent.stopped，再退出。退出码 0 表示正常停止；1 表示运行失败/身份删除/上报失败；2 表示启动参数或不可恢复输入帧错误。Ctrl+C 请求 terminate。常规宿主优先使用协议控制退出。

Node.js 宿主示例：

```powershell
node examples/aihub-host.mjs target/debug/bk-ci-agent-managed.exe examples/initialize.json
```

### 事件

agent.event 携带 type 字段：draining、taskStarted、taskFinished、duplicateTask、configurationUpdated、warning、completionUnreported。

taskFinished 表示本地执行和该次完成上报尝试结束；是否上报成功还需检查 completionUnreported/最终退出结果。completionUnreported 包含完成请求原文，供宿主保留并对账，不应直接输出到公开日志。

SDK 使用有界事件订阅，慢订阅者可能丢事件；默认程序报告 eventsLost。最终 Error::Unreported 仍保留失败的完整完成请求，受管程序在 agent.stopped.params.unreportedCompletions 中返回同一份结果。事件流不替代持久化任务状态。

## 执行与兼容约定

- TaskKey 为 projectId + buildId + vmSeqId + executeCount。运行中任务及最近 4096 个已结束任务去重；这是单进程保护，不是跨重启 exactly-once 保证。
- 普通/Docker 并发分别计数，0 沿用 Go Agent 的“不限并发”语义。配置调低只影响后续接单，不杀已有任务。
- 后台心跳更新网关、文件网关、语言、环境变量和并发数；新执行取一致配置快照。无效更新整体拒绝，不部分应用。
- SDK 只声明执行器支持且尚有容量的构建类型。升级/调试/流水线脚本能力关闭。
- 任务收到后先登记再启动，执行和上报期间均在心跳列表中，完成后释放额度。
- 成功结果默认延迟 8 秒上报，与现有 Go Agent 一致。
- 完成请求默认只发送一次，与现有实现一致。失败后停止接单、完成其他任务收尾，返回 Error::Unreported；宿主需要保留失败载荷并处理。finishAttempts > 1 仅用于后台已确认幂等的部署。
- 未知构建字段会保留并传入 worker/完成上报，以兼容服务端扩展。
- 收到 DELETE 后停止接单并收尾，以错误退出，不卸载或删除任何 Agent 文件。
- 默认 HttpBackend 使用 HTTP(S) 和系统代理环境，禁止自动重定向携带凭证。网关证书必须可验证。

## Java worker 文件与环境

Java 和 JAR 使用父进程提供的明确路径；不下载、不升级、不探测版本，也不回退 PATH/JDK。Unix 同样直接启动 Java，不加载 login shell，宿主需显式传入构建所需环境。

每次执行在 dataDir/runs/worker-* 生成独立目录，包含：

- worker 需要的最小 .agent.properties，使用 Java Properties 转义；配置来自该任务的一致快照。
- build_tmp/worker-error.log，保持现有 Go worker 的异常标记协议。
- stdout.log / stderr.log，以及 worker 自身产生的文件。

这些目录保留供诊断，由 aihub 制定保留/清理策略。程序不会清理外部工作空间。构建指定的 workspace 原样传递；未指定时使用 dataDir/workspace/<pipelineId>/src。数字分身/云桌面按业务节点 ID 的工作空间应由后台解析成明确路径后下发。

内置注入 Agent/worker 版本、构建身份、网关、语言、DEVOPS_AGENT_JDK_17_PATH（传入的 Java 路径）及兼容旧字段。宿主可以在 worker.environment 中提供额外变量；后台 config.envs 优先于这些变量；协议保留变量最后注入，避免身份被覆盖。JDK8 等额外路径由宿主按需提供。

退出结果沿用 Go 的 #10362 兼容行为：错误标记为空（或已被 worker 删除）时，JVM 非零退出码仍可视为 worker 已完成；非空则上报失败。启动失败和主动取消始终为失败。这不是任意 Java 程序的通用退出码策略，而是 BK-CI worker 适配。

# BK-CI Agent SDK for Rust

面向受管 Agent 的 Rust SDK。默认程序作为 aihub 子进程运行，继续使用现有第三方 Agent 后台协议和 Java worker。

参考同目录 Node.js SDK 的 AgentLoop / AgentApi / DefaultBuildRunner，按后台和 Go Agent 的行为实现；不依赖 Go、Node.js 进程或 FFI。

## 组件

| Crate | 用途 |
| --- | --- |
| `bk-ci-agent-sdk` | HTTP 客户端、协议类型、任务主循环、动态配置、并发与任务状态、主机信息采集、执行器接口 |
| `bk-ci-agent-worker` | Java worker 默认执行器及 worker 版本检测 |
| `bk-ci-agent-managed` | aihub 默认子进程入口：一个 Base64 JSON 参数，启动后直接运行 |

SDK 不安装全局日志、不切换进程 cwd、不接管信号、不创建自己的 Tokio runtime、不退出宿主进程。默认程序负责装配。

第一版支持普通 Java worker 构建；自定义 Executor 可以扩展 Docker 执行。升级、监控采集、JDK 下载/安装、MCP、镜像调试、流水线脚本执行不装配到默认程序。业务节点状态、权限和资源绑定由 environment/dispatch 维护，SDK 不查询数字分身后台。

## aihub 启动方式

运行入口只有一个业务参数：

```text
bk-ci-agent-managed <BASE64_JSON>
```

编码规则是 `Base64(UTF-8(JSON))`，使用标准 Base64 字母表和 padding。aihub 使用进程 API 将整个编码串作为一个参数传递，直接启动可执行文件。程序解析成功后自动采集信息、登记并开始领任务。

`examples/initialize.json` 现在只是待编码的启动配置样例，不再表示初始化协议。先替换其中的身份、凭证和本地路径：

```json
{
  "config": {
    "gateway": "http://bk-ci.example.invalid",
    "projectId": "your-project",
    "agentId": "your-agent",
    "secretKey": "replace-with-agent-secret"
  },
  "worker": {
    "javaExecutable": "C:/aihub/runtime/jdk17/bin/java.exe",
    "workerJar": "C:/aihub/runtime/worker-agent.jar",
    "dataDir": "C:/aihub/agents/instance-1"
  }
}
```

完整可选字段见 `examples/initialize.json`：

| 分组 | 内容 |
| --- | --- |
| config | 网关、Agent 身份和凭证、并发数、语言、请求超时、后台环境变量 |
| worker | Java/JAR 的明确路径、实例目录、JVM 堆大小、构建环境变量 |
| runtime（可省略） | 轮询/重试间隔、成功上报延迟、完成上报次数、近期任务去重容量 |

不接受 `metadata`、`protocolVersion` 或 JSON-RPC 包装。Java/JAR/dataDir 必须为绝对路径。不读取配置文件或 stdin，不要求父进程保持管道打开；没有 initialize、shutdown、ready 等进程间协议。

PowerShell 启动示例：

```powershell
$agentJson = Get-Content -LiteralPath examples/initialize.json -Raw
$agentArgument = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($agentJson))
& ./target/debug/bk-ci-agent-managed.exe $agentArgument
```

Node.js 宿主示例：

```powershell
node examples/aihub-host.mjs target/debug/bk-ci-agent-managed.exe examples/initialize.json
```

该示例使用 `spawn(executable, [argument], { shell: false })`。不打印配置、编码参数或 spawnargs。另提供标准 `--help` / `--version` 供人工查看；它们不启动 Agent。

## 自动采集的信息

| 字段 | 来源 |
| --- | --- |
| hostName | 本机 hostname |
| hostIp | 本地 IPv4；失败时尝试 IPv6，均失败时使用 loopback 并记录日志 |
| detectOS | 与 Go Agent 一致的 linux / windows / macos / other |
| agentVersion | Rust 程序的编译版本（Cargo package version） |
| workerVersion | 使用传入的 Java/JAR 调用 `com.tencent.devops.agent.AgentVersionKt` |
| agentInstallPath | Agent 可执行文件所在目录，与工作目录和 dataDir 区分 |
| startedUser | 当前进程用户 |
| props.arch / osVersion | 本机架构及操作系统版本 |

信息在进程启动时采集一次。worker 版本检测使用现有 Go Agent 的入口，最多等待 10 秒并限制输出大小；失败时沿用 Go 的空版本语义，日志记录失败后继续登记。版本检测使用 aihub 指定的 Java，不搜索或安装 JDK。

`props.jdkVersion` 保持空列表，Docker 属性保持默认值，不扫描机器上的 JDK 或 Docker 环境。主机信息用 `sysinfo`、`whoami`、`local-ip-address` 采集，只读取启动信息，不启用资源监控循环。

## 进程结束与日志

aihub 直接结束 Agent 进程。默认程序不注册退出信号处理器，也不等待 stdin 消息或 EOF。强杀不会执行 drain、Rust Drop 或完成上报；aihub 负责整个进程树，包括正在运行的 Java worker 和构建脚本后代。示例中的 `child.kill` 只展示 Agent 本身的终止，生产环境由 aihub 的进程树管理机制负责子进程。

后台返回 DELETE 时，Agent 自行停止接单，已领取任务收尾后以退出码 1 结束。退出码 2 表示启动参数/配置或初始化失败；1 表示运行失败、身份删除或完成上报失败；0 表示库正常返回。操作系统强杀的退出状态由操作系统决定。

Agent 日志默认以 JSON 输出到 stderr，过滤由 `RUST_LOG` 控制，默认 info。stdout 无通信输出。worker 的 stdout/stderr 写到每次执行目录。SDK 仅发出 tracing 事件，嵌入宿主自行安装 subscriber。

完成上报失败时 SDK 停止接单并收尾，返回 `Error::Unreported`。默认程序将完整载荷写到 `dataDir/unreported/finish-*.json`，日志只记录数量和文件位置，然后退出；文件名唯一，不覆盖之前运行的未上报数据。文件供宿主对账，不自动重放。强杀不保证生成这些文件。

## Rust 嵌入

```rust,ignore
let worker = JavaWorker::new(worker_options)?;
let metadata = worker.detect_metadata(env!("CARGO_PKG_VERSION")).await?;
let shutdown = Shutdown::new();
let agent = Agent::new(
    config,
    metadata,
    Arc::new(HttpBackend::new()?),
    Arc::new(worker),
    RuntimeOptions::default(),
)?;
let events = agent.subscribe(); // 可选，须在 run 前订阅
agent.run(shutdown.clone()).await?;
```

可编译示例：

```powershell
cargo run -p bk-ci-agent-worker --example embedded -- examples/initialize.json
```

`AgentMetadata::detect(agent_version, worker_version)` 也可独立调用。库层保留直接构造 metadata 的能力，供自定义执行器或测试使用；aihub 参数不暴露此项。

SDK 库仍提供 `Shutdown::stop()`（停止接单并收尾）、`terminate()`（同时取消执行器），供其他嵌入宿主使用；默认 aihub 程序不将它们映射成进程间接口。

自定义执行器实现 `Executor::execute(BuildInfo, ExecutionContext)`，返回 BuildOutcome。SDK 统一维护任务表并调用 workerBuildFinish，执行器不要重复上报。执行器应响应 cancellation，正常返回前回收直接子进程。可实现 Backend 注入其他传输；自定义 Backend 必须提供有界请求超时。

AgentEvent 是库内可选订阅，包含 ready、draining、taskStarted、taskFinished、duplicateTask、configurationUpdated、warning、completionUnreported。慢订阅者可能丢事件，最终 Error::Unreported 仍携带未上报载荷；默认程序不向父进程转发事件协议。

## 执行与兼容约定

- TaskKey 为 projectId + buildId + vmSeqId + executeCount。运行中任务和最近 4096 个已结束任务去重，属于单进程保护。
- 普通/Docker 并发分别计数，0 沿用 Go Agent 的不限并发语义；配置调低仅影响后续接单。
- 心跳更新网关、文件网关、语言、环境变量和并发数；新执行取一致快照，无效更新整体拒绝。
- SDK 仅声明执行器支持且有容量的构建类型；默认执行器不支持 Docker。
- 已领取任务在执行和完成上报期间均保留在心跳列表中，上报尝试结束后释放额度。
- 成功结果默认延迟 8 秒上报，与现有 Go Agent 一致。
- 完成请求默认只发送一次，finishAttempts > 1 仅用于后台已确认幂等的部署。
- 未知构建字段会保留并传入 worker/完成上报。
- DELETE 不卸载或删除任何 Agent 文件。
- 默认 HttpBackend 使用 HTTP(S) 和系统代理环境，禁止自动重定向携带凭证，网关证书必须可验证。

Java 和 JAR 使用 aihub 提供的明确路径；不回退 PATH/JDK，不加载 login shell。dataDir 为每个 Agent 的私有实例目录。每次执行在 dataDir/runs/worker-* 生成独立目录，包含 .agent.properties、build_tmp/worker-error.log、stdout.log、stderr.log 和 worker 自身文件，由 aihub 制定保留策略。

构建指定的 workspace 原样传递，未指定时使用 dataDir/workspace/<pipelineId>/src。业务节点工作空间由后台解析下发。worker.environment 提供额外变量，后台 config.envs 优先，协议保留变量最后注入，避免身份被覆盖。

worker 结果沿用 Go 的 #10362 行为：错误标记为空或删除时，JVM 非零退出码仍可视为 worker 已完成；非空则上报失败。启动失败和主动取消始终为失败。

## 基础组件选型

通用能力优先使用成熟 Cargo 库；依赖集中声明并由 Cargo.lock 固定。

| 能力 | 库 |
| --- | --- |
| 日志 | tracing / tracing-subscriber |
| 命令行 | clap derive |
| 启动配置解析 | base64 / serde / serde_json |
| 本机信息 | sysinfo / whoami / local-ip-address |
| 版本解析 | semver |
| 异步、子进程、取消 | tokio / tokio-util |
| HTTP / 错误 / 临时目录 | reqwest / thiserror / tempfile |

启动配置只有一个 JSON 来源，因此使用 serde 的类型与默认值即可，已移除 config 文件合并库和输入分帧依赖。

兼容性例外：worker Java Properties 暂保留小型 UTF-16 转义函数。[java-properties 2.0.0 的 writer](https://docs.rs/crate/java-properties/2.0.0/source/src/lib.rs) 使用 Windows-1252，并将非 BMP 字符写成单个 Unicode 转义，未通过真实 JVM 兼容测试。现有回归覆盖中文、补充字符、欧元符号、控制字符和属性分隔符。

## 构建和测试

要求 Rust 1.97+，已在 Windows x64 / Rust 1.97.1 验证：

```powershell
cargo build --workspace --locked
cargo test --workspace --locked
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo fmt --all --check
```

真实 JVM 测试会编译测试 JAR 并使用模拟 HTTP 后台，不连接生产服务：

```powershell
$env:BK_CI_TEST_JDK = 'D:/jdks/your-jdk17'
cargo test --workspace --locked -- --include-ignored
```

覆盖单参数启动、Base64/JSON 错误、自动主机信息、stdin 关闭后继续运行、父进程直接终止、后台删除、完成上报失败保留、worker 版本检测、后台协议/鉴权/动态并发/任务去重、Java 参数与环境/配置转义/错误标记/取消。

测试 JAR 验证启动契约；正式 worker-agent.jar 和实际 BK-CI 部署仍需接入联调。Linux/macOS 有跨平台实现，尚未实测。

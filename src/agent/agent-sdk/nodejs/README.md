# @bk-ci/agent-sdk (Node.js)

BK-CI 第三方构建 Agent 的 **Node.js SDK**。它把 Go 版 agent 的「主循环编排」沉淀为通用骨架，
把「平台/业务特定动作」抽象为 `AgentHandler` 接口。你只需实现少数几个方法，即可获得完整的
**领任务、上报心跳、采集监控** 能力，无需重写主循环、轮询节奏、并发判断与错误处理。

- 零运行时依赖（仅用 Node 内置 `http`/`https`）
- 纯库，无外部进程、无 FFI
- TypeScript 编写，自带类型提示
- 协议与 Go agent 严格对齐（同一套 `X-DEVOPS-*` 鉴权头 + JSON）

## 安装

```bash
npm install @bk-ci/agent-sdk
```

## 快速开始

```ts
import { AgentConfig, AgentHandler, AgentLoop } from '@bk-ci/agent-sdk';

class MyHandler implements AgentHandler {
  // 实现下列方法……（见「接口说明」）
}

const config = new AgentConfig({
  gateway: 'http://your-bk-ci-gateway',
  projectId: 'your-project',
  agentId: 'your-agent-id',
  secretKey: 'your-secret-key',
  parallelTaskCount: 4,
});

const loop = new AgentLoop({ config, handler: new MyHandler() });
await loop.run(); // 长驻，直到 loop.stop()
```

也可以直接从 `.agent.properties` 加载配置：

```ts
const config = AgentConfig.fromPropertiesFile('/data/agent/.agent.properties');
```

## 架构

```
AgentLoop (通用骨架)                    ← SDK 提供，逻辑与 Go agent 一致
  ├─ startup 带重试
  ├─ 5s 轮询: doAsk
  │    ├─ genAskEnable  (调 Handler 状态查询)
  │    ├─ 组装心跳       (通用字段 + Handler.collectHeartExtra)
  │    ├─ api.ask()      → 后台
  │    ├─ 错误/AgentStatus 判断 (删除→onAgentDeleted)
  │    └─ doAgentJob 分发 (异常隔离)
  │         ├─ heartbeat → onHeartbeatResp
  │         ├─ build     → onBuild
  │         ├─ upgrade   → onUpgrade
  │         ├─ pipeline  → onPipeline
  │         └─ debug     → onImageDebug
  └─ 可选 monitor 采集循环

AgentHandler (接口)                     ← 你来实现
```

## 接口说明（AgentHandler）

| 方法 | 类别 | 职责 |
|------|------|------|
| `onStartup()` | 生命周期 | 返回启动信息(OS/IP/版本)，框架据此上报，失败自动重试 |
| `collectHeartExtra(ctx, upgradeEnable)` | 心跳采集 | 返回心跳的平台特定字段 + 升级信息 |
| `isUpgrading()` | 状态查询 | 是否正在升级 |
| `checkParallelTaskCount()` | 状态查询 | 返回 `[dockerCanRun, normalCanRun]` |
| `hasRunningJob()` | 状态查询 | 是否有运行中任务 |
| `pipelineEnabled()` / `dockerDebugEnabled()` | 状态查询 | 能力开关 |
| `onBuild(build)` | 动作 | **核心**：拉起执行器/worker 跑构建 |
| `onUpgrade(item, hasBuild)` | 动作 | 处理升级 |
| `onPipeline(pipeline)` | 动作 | 执行流水线脚本 |
| `onImageDebug(debug)` | 动作 | 启动调试容器 |
| `onHeartbeatResp(resp)` | 钩子 | 同步网关/并发数/环境变量等配置 |
| `onAgentDeleted()` | 钩子 | Agent 被删除时处理 |
| `onAskResp(resp)` | 钩子(可选) | ask 响应通用后处理 |

任务动作方法都被框架用异常隔离包裹（对应 Go 的 `safeGo` + recover），单个任务异常不会中断主循环。

## 复用 API

`AgentLoop.getApi()` 返回 `AgentApi`，可在任务执行过程中上报状态：

```ts
const api = loop.getApi();
await api.workerBuildFinish({ ...buildInfo, success: true, message: 'ok' });
await api.addLogLine(buildId, { message: 'building...', timestamp: Date.now(), tag: '', jobId: '', logType: LogType.Log }, vmSeqId);
await api.updatePipelineStatus({ seqId, status: 'success', response: '' });
```

### 检测 worker-agent.jar 版本

SDK 默认提供 `detectWorkerVersion`。传入 JDK17 和 `worker-agent.jar` 路径后，
它会调用 jar 中的 `com.tencent.devops.agent.AgentVersionKt` 获取版本：

```ts
import { detectWorkerVersion } from '@bk-ci/agent-sdk';

const workerVersion = await detectWorkerVersion({
  jdk17Path: '/data/agent/jdk17', // 也可以直接传 /data/agent/jdk17/bin/java
  workerJarPath: '/data/agent/worker-agent.jar',
});
```

返回值兼容 `v1.2.3`、`v1.2.3-RELEASE`、`v1.2.3-SNAPSHOT`、
`v1.2.3-beta.4` 等格式；JDK、jar、命令执行或版本解析失败时返回空字符串。

需要使用官方 `worker-agent.jar` 的默认升级模式时，可使用
`DefaultWorkerJarManager`：启动前调用 `initialize()` 获取当前版本并在文件缺失时自愈；
收到后台 worker 升级指令后调用 `upgrade()`。候选 jar 会先下载到 `upgrade` 目录，
通过版本与 MD5 校验后才替换正式文件。完整的暂停接单、动态版本与
`finishUpgrade` 上报方式见 `examples/basic.ts`。

## 协议对齐

- 鉴权头：`X-DEVOPS-BUILD-TYPE=AGENT` / `X-DEVOPS-PROJECT-ID` / `X-DEVOPS-AGENT-ID` / `X-DEVOPS-AGENT-SECRET-KEY`；日志接口额外带 `X-DEVOPS-BUILD-ID` / `X-DEVOPS-VM-SID`
- 核心接口：`POST /ms/dispatch/api/buildAgent/agent/thirdPartyAgent/ask` 等（见 `src/api.ts`）
- 返回结构：`{ data, status, message }`，`status===0` 为成功；ask 额外带 `agentStatus`（`IMPORT_OK`/`DELETE`）

## 开发

```bash
npm install
npm run build      # 编译到 dist/
npx ts-node examples/basic.ts   # 运行示例
```

## 与 Go agent 的对应关系

| 本 SDK | Go agent |
|--------|----------|
| `AgentLoop` | `src/pkg/agent/agent.go` (Run/doAsk/doAgentJob) |
| `genAskEnable/checkBuildType` | `src/pkg/agent/ask.go` |
| `AgentConfig` | `src/pkg/config/config.go` |
| `AgentApi` | `src/pkg/api/api.go` |
| `types.ts` | `src/pkg/api/type.go` |
| `HttpClient`/`DevopsResult` | `src/pkg/util/httputil` |

## License

MIT

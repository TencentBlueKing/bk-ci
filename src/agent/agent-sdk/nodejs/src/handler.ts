/*
 * BK-CI Agent SDK - Handler 接口定义
 *
 * 这是 SDK 的核心扩展点：接入方（各业务/各平台）只需实现本接口的少数几个方法，
 * 即可复用 AgentLoop 提供的通用主循环（轮询、领任务、上报心跳、错误处理）。
 *
 * 设计对应 Go agent 的可插拔点：doAgentJob 分发的 5 个动作 + 状态查询 + 采集。
 * 通用编排全部在 AgentLoop 里，Handler 只承担平台/业务特定逻辑。
 */

import {
  AgentHeartbeatInfo,
  AgentHeartbeatResponse,
  AgentPropsInfo,
  AskResp,
  ImageDebug,
  ThirdPartyBuildInfo,
  ThirdPartyDockerTaskInfo,
  ThirdPartyTaskInfo,
  UpgradeInfo,
  UpgradeItem,
} from './types';

/**
 * 心跳中由框架预先填充的通用字段。Handler 在 collectHeartExtra 中拿到它，
 * 只需补充平台特定字段（jdkVersion / dockerInitFile 等）。
 */
export interface HeartbeatContext {
  /** 当前正在运行的普通构建任务列表 */
  taskList: ThirdPartyTaskInfo[];
  /** 当前正在运行的 Docker 构建任务列表 */
  dockerTaskList: ThirdPartyDockerTaskInfo[];
}

/**
 * AgentHandler：接入方需要实现的接口。
 * 方法分四类：生命周期、心跳采集、状态查询、任务动作。
 * 除标注为可选(?)外，其余为必须实现。所有动作方法可返回 Promise。
 */
export interface AgentHandler {
  // ── 生命周期 ──

  /**
   * 上报启动信息（OS/IP/版本）。抛异常时框架会按固定间隔重试直到成功。
   * 返回启动上报所需的部分环境信息，框架据此组装 ThirdPartyAgentStartInfo。
   */
  onStartup(): Promise<StartupInfo> | StartupInfo;

  // ── 心跳采集 ──

  /**
   * 采集心跳的平台特定字段。框架已填好通用字段（版本/host/并发数/taskList），
   * Handler 返回需要合并进心跳体的补充字段，以及（当 upgradeEnable 为真时）升级信息。
   */
  collectHeartExtra(ctx: HeartbeatContext, upgradeEnable: boolean): HeartExtra | Promise<HeartExtra>;

  // ── 状态查询（供框架组装 AskEnable，即"我能不能接新任务"）──

  /** 是否正在升级中（升级期间禁止接新任务） */
  isUpgrading(): boolean;
  /** 并发是否还有空位：返回 [docker 可运行, 普通可运行] */
  checkParallelTaskCount(): [dockerCanRun: boolean, normalCanRun: boolean];
  /** 是否有运行中的任务（有则不允许升级） */
  hasRunningJob(): boolean;
  /** 是否启用流水线能力 */
  pipelineEnabled(): boolean;
  /** 是否启用 Docker 调试能力 */
  dockerDebugEnabled(): boolean;

  // ── 任务动作（doAgentJob 分发目标，框架已用异常隔离包裹）──

  /** 领取到构建任务：真正去拉起执行器/worker 并管理其生命周期 */
  onBuild(build: ThirdPartyBuildInfo): void | Promise<void>;
  /** 收到升级指令 */
  onUpgrade(upgrade: UpgradeItem, hasBuild: boolean): void | Promise<void>;
  /** 收到流水线执行指令（Go 中为 map[string]any，故为任意对象） */
  onPipeline(pipeline: Record<string, unknown>): void | Promise<void>;
  /** 收到镜像调试指令 */
  onImageDebug(debug: ImageDebug): void | Promise<void>;

  // ── 其它钩子 ──

  /** 处理心跳响应：同步网关/并发数/环境变量等配置到本地 */
  onHeartbeatResp(resp: AgentHeartbeatResponse): void | Promise<void>;
  /** Agent 被后台删除时的处理（如触发卸载） */
  onAgentDeleted(): void | Promise<void>;
  /** ask 响应的通用后处理钩子（可选，如平台特有的一次性逻辑） */
  onAskResp?(resp: AskResp): void | Promise<void>;
}

/** onStartup 返回的启动信息 */
export interface StartupInfo {
  hostName: string;
  hostIp: string;
  detectOS: string;
  masterVersion: string;
  /** worker/slave 版本 */
  version: string;
}

/** collectHeartExtra 返回的心跳补充字段 */
export interface HeartExtra {
  masterVersion: string;
  slaveVersion: string;
  hostName: string;
  agentIp: string;
  agentInstallPath: string;
  startedUser: string;
  props: AgentPropsInfo;
  /** 当 upgradeEnable 为真时返回升级信息，否则可为 null */
  upgrade?: UpgradeInfo | null;
  /** 允许覆盖框架默认的心跳字段（高级用法，一般不用） */
  override?: Partial<AgentHeartbeatInfo>;
}

/*
 * BK-CI Agent SDK - 主循环骨架 AgentLoop
 *
 * 对应 Go agent 的 src/pkg/agent/{agent.go, ask.go}。
 * 承载全部"通用编排"逻辑：startup 重试、固定间隔轮询、genAskEnable 组装、
 * 心跳通用字段组装、doAsk 的错误/状态判断、doAgentJob 分发、异常隔离(safeGo)。
 *
 * 接入方通过实现 AgentHandler 注入平台/业务特定逻辑。
 */

import { AgentApi } from './api';
import { AgentConfig } from './config';
import { HttpClient, isAgentDelete, isNotOk } from './httpClient';
import { AgentHandler } from './handler';
import { AgentStatus } from './config';
import {
  AgentHeartbeatInfo,
  AskEnable,
  AskInfo,
  AskResp,
  BuildJobType,
  UpgradeInfo,
} from './types';

/** 轻量日志接口，接入方可注入自定义实现，默认走 console。 */
export interface Logger {
  debug(...args: unknown[]): void;
  info(...args: unknown[]): void;
  warn(...args: unknown[]): void;
  error(...args: unknown[]): void;
}

const defaultLogger: Logger = {
  debug: (...a) => console.debug('[agent-sdk][debug]', ...a),
  info: (...a) => console.info('[agent-sdk][info]', ...a),
  warn: (...a) => console.warn('[agent-sdk][warn]', ...a),
  error: (...a) => console.error('[agent-sdk][error]', ...a),
};

export interface AgentLoopOptions {
  config: AgentConfig;
  handler: AgentHandler;
  /** 轮询间隔（毫秒），默认 5000，对应 Go 主循环的 5s */
  intervalMs?: number;
  /** startup 失败重试间隔（毫秒），默认 5000 */
  startupRetryMs?: number;
  logger?: Logger;
  httpClient?: HttpClient;
  /** 可选：监控采集回调。若提供，会在启动后以独立循环周期调用（对应 Go monitor.Collect） */
  monitorFn?: () => void | Promise<void>;
  /** 监控采集间隔（毫秒），默认 60000（1 分钟） */
  monitorIntervalMs?: number;
  /** 可选：启动副作用钩子（对应 Go initModules/cron 等） */
  onInit?: () => void | Promise<void>;
}

export class AgentLoop {
  private readonly config: AgentConfig;
  private readonly handler: AgentHandler;
  private readonly intervalMs: number;
  private readonly startupRetryMs: number;
  private readonly logger: Logger;
  private readonly api: AgentApi;
  private readonly monitorFn?: () => void | Promise<void>;
  private readonly monitorIntervalMs: number;
  private readonly onInit?: () => void | Promise<void>;

  private running = false;
  private stopped = false;
  private timers: NodeJS.Timeout[] = [];

  constructor(opts: AgentLoopOptions) {
    this.config = opts.config;
    this.handler = opts.handler;
    this.intervalMs = opts.intervalMs ?? 5000;
    this.startupRetryMs = opts.startupRetryMs ?? 5000;
    this.logger = opts.logger ?? defaultLogger;
    const client = opts.httpClient ?? new HttpClient({ timeoutMs: this.config.timeoutSec * 1000 });
    this.api = new AgentApi(this.config, client);
    this.monitorFn = opts.monitorFn;
    this.monitorIntervalMs = opts.monitorIntervalMs ?? 60_000;
    this.onInit = opts.onInit;
  }

  /** 供接入方直接复用的 API 封装（上报日志/构建完成/流水线状态等） */
  getApi(): AgentApi {
    return this.api;
  }

  /**
   * 启动并进入主循环（长驻）。返回的 Promise 在 stop() 被调用后 resolve。
   * 对应 Go 的 agent.Run()。
   */
  async run(): Promise<void> {
    if (this.running) {
      throw new Error('AgentLoop already running');
    }
    this.running = true;
    this.stopped = false;

    await this.startup();

    if (this.monitorFn) {
      this.startMonitorLoop();
    }
    if (this.onInit) {
      await this.safeCall('onInit', () => this.onInit!());
    }

    // 主循环：对应 Go 的 for { doAsk(); sleep 5s }
    while (!this.stopped) {
      await this.doAsk();
      if (this.stopped) break;
      await this.sleep(this.intervalMs);
    }
    this.running = false;
  }

  /** 停止主循环。 */
  stop(): void {
    this.stopped = true;
    for (const t of this.timers) clearTimeout(t);
    this.timers = [];
  }

  // ── startup 带重试（对应 Go Run 中的启动重试段）──
  private async startup(): Promise<void> {
    // 首次失败后无限重试 + 固定间隔，直到成功
    for (;;) {
      try {
        const info = await this.handler.onStartup();
        const result = await this.api.agentStartup({
          hostname: info.hostName,
          hostIp: info.hostIp,
          detectOS: info.detectOS,
          masterVersion: info.masterVersion,
          version: info.version,
        });
        if (isNotOk(result)) {
          throw new Error(`agent startup result failed: ${result.message}`);
        }
        this.logger.info('agent startup success');
        return;
      } catch (e) {
        this.logger.error('agent startup failed', errMsg(e));
        if (this.stopped) return;
        await this.sleep(this.startupRetryMs);
      }
    }
  }

  // ── 一次 ask 轮询（对应 Go doAsk）──
  private async doAsk(): Promise<void> {
    const enable = await this.genAskEnable();
    let heart: AgentHeartbeatInfo;
    let upgrade: UpgradeInfo | null;
    try {
      const built = await this.buildHeartbeat(enable.upgrade);
      heart = built.heart;
      upgrade = built.upgrade;
    } catch (e) {
      this.logger.error('build heartbeat failed', errMsg(e));
      return;
    }

    const info: AskInfo = { askEnable: enable, heartbeat: heart, upgrade };

    let result;
    try {
      result = await this.api.ask(info);
    } catch (e) {
      this.logger.error('ask request failed', errMsg(e));
      return;
    }

    if (isNotOk(result)) {
      this.logger.error('ask request result failed:', result.message);
      return;
    }

    if (result.agentStatus !== AgentStatus.ImportOk) {
      this.logger.error(`agent status [${result.agentStatus}] not ok`);
      if (isAgentDelete(result)) {
        this.logger.warn('agent has been deleted');
        await this.safeCall('onAgentDeleted', () => this.handler.onAgentDeleted());
      }
      return;
    }

    const resp = (result.data ?? {}) as AskResp;

    if (this.handler.onAskResp) {
      await this.safeCall('onAskResp', () => this.handler.onAskResp!(resp));
    }

    this.doAgentJob(enable, resp);
  }

  // ── 分发各类任务（对应 Go doAgentJob），每个动作异常隔离，不阻塞主循环 ──
  private doAgentJob(enable: AskEnable, resp: AskResp): void {
    if (resp.heartbeat) {
      const hb = resp.heartbeat;
      this.safeGo('onHeartbeatResp', () => this.handler.onHeartbeatResp(hb));
    }

    const hasBuild = enable.build !== BuildJobType.None && !!resp.build;
    if (hasBuild && resp.build) {
      const build = resp.build;
      this.safeGo('onBuild', () => this.handler.onBuild(build));
    }

    if (enable.upgrade && resp.upgrade) {
      const up = resp.upgrade;
      this.safeGo('onUpgrade', () => this.handler.onUpgrade(up, hasBuild));
    }

    if (enable.pipeline && resp.pipeline) {
      const pl = resp.pipeline;
      this.safeGo('onPipeline', () => this.handler.onPipeline(pl));
    }

    if (enable.dockerDebug && resp.debug) {
      const dbg = resp.debug;
      this.safeGo('onImageDebug', () => this.handler.onImageDebug(dbg));
    }
  }

  // ── 组装 AskEnable（对应 Go genAskEnable + checkBuildType）──
  private async genAskEnable(): Promise<AskEnable> {
    return {
      build: this.checkBuildType(),
      upgrade: this.checkUpgrade(),
      dockerDebug: this.handler.dockerDebugEnabled(),
      pipeline: this.handler.pipelineEnabled(),
    };
  }

  private checkBuildType(): BuildJobType {
    if (this.handler.isUpgrading()) {
      return BuildJobType.None;
    }
    const [dockerCanRun, normalCanRun] = this.handler.checkParallelTaskCount();
    if (!dockerCanRun && !normalCanRun) return BuildJobType.None;
    if (dockerCanRun && normalCanRun) return BuildJobType.All;
    if (normalCanRun) return BuildJobType.Binary;
    return BuildJobType.Docker;
  }

  private checkUpgrade(): boolean {
    if (this.handler.hasRunningJob()) return false;
    if (this.handler.isUpgrading()) return false;
    return true;
  }

  // ── 组装心跳体：通用字段由框架填，平台字段由 handler 提供 ──
  private async buildHeartbeat(
    upgradeEnable: boolean
  ): Promise<{ heart: AgentHeartbeatInfo; upgrade: UpgradeInfo | null }> {
    const extra = await this.handler.collectHeartExtra(
      { taskList: [], dockerTaskList: [] },
      upgradeEnable
    );

    const heart: AgentHeartbeatInfo = {
      masterVersion: extra.masterVersion,
      slaveVersion: extra.slaveVersion,
      hostName: extra.hostName,
      agentIp: extra.agentIp,
      parallelTaskCount: this.config.parallelTaskCount,
      agentInstallPath: extra.agentInstallPath,
      startedUser: extra.startedUser,
      taskList: extra.override?.taskList ?? [],
      props: extra.props,
      dockerParallelTaskCount: this.config.dockerParallelTaskCount,
      dockerTaskList: extra.override?.dockerTaskList ?? [],
      errorExitData: extra.override?.errorExitData ?? null,
      ...(extra.override ?? {}),
    };

    return { heart, upgrade: upgradeEnable ? extra.upgrade ?? null : null };
  }

  // ── 监控采集独立循环（对应 Go safeGo("monitor", monitor.Collect)）──
  private startMonitorLoop(): void {
    const tick = async (): Promise<void> => {
      if (this.stopped) return;
      await this.safeCall('monitor', () => this.monitorFn!());
      if (this.stopped) return;
      const t = setTimeout(tick, this.monitorIntervalMs);
      this.timers.push(t);
    };
    // 立即执行首轮，随后按间隔
    void tick();
  }

  // ── 异常隔离：异步触发动作，捕获异常防止影响主循环（对应 Go safeGo）──
  private safeGo(name: string, fn: () => void | Promise<void>): void {
    Promise.resolve()
      .then(fn)
      .catch((e) => {
        this.logger.error(`task [${name}] error:`, errMsg(e));
      });
  }

  // ── 同步等待型安全调用：await 但吞掉异常 ──
  private async safeCall(name: string, fn: () => void | Promise<void>): Promise<void> {
    try {
      await fn();
    } catch (e) {
      this.logger.error(`hook [${name}] error:`, errMsg(e));
    }
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => {
      const t = setTimeout(resolve, ms);
      this.timers.push(t);
    });
  }
}

function errMsg(e: unknown): string {
  if (e instanceof Error) return e.message;
  return String(e);
}

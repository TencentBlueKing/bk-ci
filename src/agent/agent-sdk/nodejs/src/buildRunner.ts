/*
 * BK-CI Agent SDK - 参考/默认 BuildRunner
 *
 * 开箱即用的构建分发器：接入方在自己的 AgentHandler.onBuild 里调用 runner.onBuild(build)。
 * 内部按 dockerBuildInfo 是否存在分发到 runDockerBuild 或 runWorkerBuild，
 * 结束后调用 api.workerBuildFinish 上报（成功时先 sleep 8s，对齐 Go workerBuildFinish）。
 * 同时维护 taskList / dockerTaskList，供心跳上报（collectHeartExtra）读取。
 *
 * 该实现是可选的参考件；接入方也可绕过它，直接调用 worker.ts / dockerBuild.ts 的底层函数
 * 并自行编排（例如替换成自定义 worker）。
 */

import { AgentApi } from './api';
import { DockerRunner, DockerEventLogFn } from './dockercli';
import { runDockerBuild } from './dockerBuild';
import { runWorkerBuild } from './worker';
import {
  ThirdPartyBuildInfo,
  ThirdPartyBuildWithStatus,
  ThirdPartyDockerTaskInfo,
  ThirdPartyTaskInfo,
} from './types';

export interface BuildRunnerOptions {
  /** 用于上报 workerBuildFinish 的 API。 */
  api: AgentApi;
  /** agent 工作目录。 */
  workDir: string;
  /** worker-agent.jar 绝对路径。 */
  workerJarPath: string;
  /** JDK17 java 可执行文件或目录（worker 构建）。 */
  jdk17Path?: string;
  /** JDK8 java 可执行文件或目录（worker 构建）。 */
  jdk8Path?: string;
  /** JDK17 目录（docker 构建挂载用）。 */
  jdk17DirPath?: string;
  /** JDK8 目录（docker 构建挂载用）。 */
  jdk8DirPath?: string;
  /** docker init 脚本绝对路径（挂载到 /data/init.sh）。 */
  dockerInitScriptPath?: string;
  /** 后台网关地址。 */
  gateway: string;
  /** 文件网关地址。 */
  fileGateway?: string;
  /** 项目 id。 */
  projectId: string;
  /** agent id（用于自动补齐 worker 的 .agent.properties）。 */
  agentId?: string;
  /** agent 密钥（用于自动补齐 worker 的 .agent.properties）。 */
  secretKey?: string;
  /** agent 版本。 */
  agentVersion: string;
  /** worker 版本。 */
  workerVersion: string;
  /** 语言，默认 zh_CN。 */
  language?: string;
  /** 普通构建最大并发，默认 4。 */
  parallelTaskCount?: number;
  /** docker 构建最大并发，默认 4。 */
  dockerParallelTaskCount?: number;
  /** 是否探测用户 SHELL（worker Unix login shell）。 */
  detectShell?: boolean;
  /** 额外注入 worker/容器的环境变量。 */
  extraEnv?: Record<string, string>;
  /** 日志上报回调（用于把构建/docker 日志转发到后台）。 */
  postLog?: (build: ThirdPartyBuildInfo, message: string) => void;
  /** 轻量日志回调，默认 console。 */
  logFn?: (msg: string) => void;
}

export class DefaultBuildRunner {
  private readonly opts: BuildRunnerOptions;
  private readonly parallelTaskCount: number;
  private readonly dockerParallelTaskCount: number;
  private readonly log: (msg: string) => void;

  // 运行中的普通构建：buildId -> task
  private readonly tasks = new Map<string, ThirdPartyTaskInfo>();
  // 运行中的 docker 构建：buildId -> task
  private readonly dockerTasks = new Map<string, ThirdPartyDockerTaskInfo>();

  constructor(opts: BuildRunnerOptions) {
    this.opts = opts;
    this.parallelTaskCount = opts.parallelTaskCount ?? 4;
    this.dockerParallelTaskCount = opts.dockerParallelTaskCount ?? 4;
    this.log = opts.logFn ?? ((m) => console.info('[agent-sdk][runner]', m));
  }

  /** 当前正在运行的普通构建任务（供心跳）。 */
  getTaskList(): ThirdPartyTaskInfo[] {
    return Array.from(this.tasks.values());
  }

  /** 当前正在运行的 docker 构建任务（供心跳）。 */
  getDockerTaskList(): ThirdPartyDockerTaskInfo[] {
    return Array.from(this.dockerTasks.values());
  }

  /** 是否有运行中的任务（供 hasRunningJob）。 */
  hasRunningJob(): boolean {
    return this.tasks.size > 0 || this.dockerTasks.size > 0;
  }

  /**
   * 并发是否还有空位（供 checkParallelTaskCount）：返回 [docker 可运行, 普通可运行]。
   */
  checkParallelTaskCount(): [dockerCanRun: boolean, normalCanRun: boolean] {
    const dockerCanRun =
      this.dockerParallelTaskCount === 0 || this.dockerTasks.size < this.dockerParallelTaskCount;
    const normalCanRun =
      this.parallelTaskCount === 0 || this.tasks.size < this.parallelTaskCount;
    return [dockerCanRun, normalCanRun];
  }

  /**
   * 领取到构建任务后调用：分发执行并在结束时上报。
   * 该方法内部已做异常隔离，不会抛出。
   */
  async onBuild(build: ThirdPartyBuildInfo): Promise<void> {
    if (build.dockerBuildInfo) {
      await this.runDocker(build);
    } else {
      await this.runNormal(build);
    }
  }

  private async runNormal(build: ThirdPartyBuildInfo): Promise<void> {
    this.tasks.set(build.buildId, {
      projectId: build.projectId,
      buildId: build.buildId,
      vmSeqId: build.vmSeqId,
      workspace: build.workspace,
    });
    try {
      const result = await runWorkerBuild({
        buildInfo: build,
        workDir: this.opts.workDir,
        jdk17Path: this.opts.jdk17Path,
        jdk8Path: this.opts.jdk8Path,
        workerJarPath: this.opts.workerJarPath,
        gateway: this.opts.gateway,
        fileGateway: this.opts.fileGateway,
        projectId: this.opts.projectId,
        agentId: this.opts.agentId,
        secretKey: this.opts.secretKey,
        agentVersion: this.opts.agentVersion,
        workerVersion: this.opts.workerVersion,
        language: this.opts.language,
        extraEnv: this.opts.extraEnv,
        detectShell: this.opts.detectShell,
        logFn: this.log,
      });
      await this.finish(build, result.success, result.message);
    } catch (e) {
      await this.finish(build, false, errMsg(e));
    } finally {
      this.tasks.delete(build.buildId);
    }
  }

  private async runDocker(build: ThirdPartyBuildInfo): Promise<void> {
    this.dockerTasks.set(build.buildId, {
      projectId: build.projectId,
      buildId: build.buildId,
      vmSeqId: build.vmSeqId,
    });
    try {
      if (!this.opts.dockerInitScriptPath) {
        await this.finish(build, false, 'dockerInitScriptPath not configured');
        return;
      }
      const eventf: DockerEventLogFn = (entry) => {
        if (this.opts.postLog) this.opts.postLog(build, `[docker] ${entry.message}`);
        this.log(`[docker][${entry.level}] ${entry.message}`);
      };
      const runner = new DockerRunner(this.opts.workDir, eventf);
      const result = await runDockerBuild(runner, {
        buildInfo: build,
        workDir: this.opts.workDir,
        jdk17DirPath: this.opts.jdk17DirPath,
        jdk8DirPath: this.opts.jdk8DirPath,
        workerJarPath: this.opts.workerJarPath,
        dockerInitScriptPath: this.opts.dockerInitScriptPath,
        gateway: this.opts.gateway,
        projectId: this.opts.projectId,
        extraEnv: this.opts.extraEnv,
        postLog: this.opts.postLog
          ? (msg) => this.opts.postLog!(build, msg)
          : undefined,
      });
      await this.finish(build, result.success, result.message);
    } catch (e) {
      await this.finish(build, false, errMsg(e));
    } finally {
      this.dockerTasks.delete(build.buildId);
    }
  }

  /** 上报构建完成：成功时先 sleep 8s（对齐 Go workerBuildFinish）。 */
  private async finish(build: ThirdPartyBuildInfo, success: boolean, message: string): Promise<void> {
    if (success) {
      await sleep(8000);
    }
    const body: ThirdPartyBuildWithStatus = {
      ...build,
      success,
      message,
      error: null,
    };
    try {
      const result = await this.opts.api.workerBuildFinish(body);
      if (result.status !== 0) {
        this.log(`workerBuildFinish result not ok: ${result.message}`);
      }
    } catch (e) {
      this.log(`workerBuildFinish failed: ${errMsg(e)}`);
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : String(e);
}

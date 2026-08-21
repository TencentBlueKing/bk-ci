/*
 * 可运行示例：实现 AgentHandler，并用 DefaultBuildRunner 拉起真实 worker / docker 构建。
 *
 * 运行前请准备好后台网关与鉴权信息。两种方式提供配置：
 *   1) AgentConfig.fromPropertiesFile('/path/to/.agent.properties')
 *   2) 直接 new AgentConfig({...})（本示例采用）
 *
 * 用 ts-node 运行：  npx ts-node examples/basic.ts
 */

import * as os from 'os';
import * as path from 'path';
import {
  AgentConfig,
  AgentHandler,
  AgentLoop,
  BuildJobType,
  HeartExtra,
  HeartbeatContext,
  ImageDebug,
  StartupInfo,
  ThirdPartyBuildInfo,
  UpgradeItem,
  AgentHeartbeatResponse,
  registry,
  AgentApi,
  HttpClient,
  DefaultBuildRunner,
  downloadWorkerJar,
  downloadDockerInitFile,
  LogType,
} from '../src';

const AGENT_VERSION = '1.0.0-nodejs-sdk';
const WORKER_VERSION = '';
// ── Agent配置（TODO: 按你的环境填写）──────────────────────────────
const GATEWAY = ''; // TODO: 填写后台网关地址
const TOKEN = ''; // TODO: 填写 token/deviceId/userId 鉴权相关
const DEVICE_ID = ''; // TODO: 填写 token/deviceId/userId  鉴权相关
const USER_ID = ''; // TODO: 填写 token/deviceId/userId  鉴权相关
// ── 实机路径配置（TODO: 按你的环境填写）──────────────────────────────
const WORK_DIR = process.cwd(); // TODO: 填写 agent 工作目录
const WORKER_JAR_PATH = path.join(WORK_DIR, 'worker-agent.jar'); // TODO: 填写 worker-agent.jar 路径
const JDK17_PATH = ''; // TODO: 填写 jdk17 的 java 可执行文件或 JDK 目录
const JDK8_PATH = ''; // TODO: 填写 jdk8 的 java 可执行文件或 JDK 目录（可选）
const JDK17_DIR_PATH = ''; // TODO: docker 构建挂载用的 jdk17 目录（可选）
const JDK8_DIR_PATH = ''; // TODO: docker 构建挂载用的 jdk8 目录（可选）
const DOCKER_INIT_SCRIPT_PATH = path.join(WORK_DIR, 'agent_docker_init.sh'); // TODO: docker init 脚本路径
const ENABLE_DOCKER_BUILD = false; // TODO: 是否启用 docker 构建
const PARALLEL_TASK_COUNT = 4;
const DOCKER_PARALLEL_TASK_COUNT = 4;

/**
 * Handler：把构建动作委托给 DefaultBuildRunner，
 * 其它生命周期/心跳/状态查询仍由本类实现。
 */
class DemoHandler implements AgentHandler {
  private upgrading = false;

  constructor(private readonly runner: DefaultBuildRunner) { }

  onStartup(): StartupInfo {
    return {
      hostName: os.hostname(),
      hostIp: firstNonLoopbackIp(),
      detectOS: `${os.platform()}_${os.release()}`,
      masterVersion: AGENT_VERSION,
      version: WORKER_VERSION,
    };
  }

  collectHeartExtra(_ctx: HeartbeatContext, upgradeEnable: boolean): HeartExtra {
    return {
      masterVersion: AGENT_VERSION,
      slaveVersion: WORKER_VERSION,
      hostName: os.hostname(),
      agentIp: firstNonLoopbackIp(),
      agentInstallPath: WORK_DIR,
      startedUser: os.userInfo().username,
      props: {
        arch: os.arch(),
        jdkVersion: [],
        dockerInitFileMd5: { fileMd5: '', needUpgrade: false },
        osVersion: os.release(),
      },
      // 由 runner 维护正在运行的任务列表，供心跳上报
      override: {
        taskList: this.runner.getTaskList(),
        dockerTaskList: this.runner.getDockerTaskList(),
      },
      upgrade: upgradeEnable
        ? {
          workerVersion: WORKER_VERSION,
          goAgentVersion: AGENT_VERSION,
          jdkVersion: [],
          dockerInitFileInfo: { fileMd5: '', needUpgrade: false },
        }
        : null,
    };
  }

  isUpgrading(): boolean {
    return this.upgrading;
  }

  checkParallelTaskCount(): [boolean, boolean] {
    return this.runner.checkParallelTaskCount();
  }

  hasRunningJob(): boolean {
    return this.runner.hasRunningJob();
  }

  pipelineEnabled(): boolean {
    return false;
  }

  dockerDebugEnabled(): boolean {
    return false;
  }

  /** 领取到构建任务：直接委托给 runner，它会分发 worker/docker 并上报 finish。 */
  async onBuild(build: ThirdPartyBuildInfo): Promise<void> {
    console.log(`[demo] 领取到构建任务 buildId=${build.buildId} vmSeqId=${build.vmSeqId}`);
    await this.runner.onBuild(build);
    console.log(`[demo] 构建结束 buildId=${build.buildId}`);
  }

  onUpgrade(upgrade: UpgradeItem, hasBuild: boolean): void {
    console.log('[demo] 收到升级指令', upgrade, 'hasBuild=', hasBuild);
  }

  onPipeline(pipeline: Record<string, unknown>): void {
    console.log('[demo] 收到流水线指令', pipeline);
  }

  onImageDebug(debug: ImageDebug): void {
    console.log('[demo] 收到镜像调试指令 debugId=', debug.debugId);
  }

  onHeartbeatResp(resp: AgentHeartbeatResponse): void {
    console.log('[demo] 心跳响应，parallelTaskCount=', resp.parallelTaskCount);
  }

  onAgentDeleted(): void {
    console.warn('[demo] Agent 已被后台删除');
  }
}

function firstNonLoopbackIp(): string {
  const ifaces = os.networkInterfaces();
  for (const name of Object.keys(ifaces)) {
    for (const ni of ifaces[name] ?? []) {
      if (ni.family === 'IPv4' && !ni.internal) return ni.address;
    }
  }
  return '127.0.0.1';
}

async function main(): Promise<void> {
  const regisResp = await registry(GATEWAY, { token: TOKEN, deviceId: DEVICE_ID, userId: USER_ID });
  const config = new AgentConfig({
    gateway: GATEWAY,
    projectId: regisResp.projectId,
    agentId: regisResp.agentId,
    secretKey: regisResp.secretKey,
    fileGateway: regisResp.fileGateway,
    parallelTaskCount: regisResp.parallelTaskCount ?? PARALLEL_TASK_COUNT,
    dockerParallelTaskCount: regisResp.dockerParallelTaskCount ?? DOCKER_PARALLEL_TASK_COUNT,
    enableDockerBuild: ENABLE_DOCKER_BUILD,
  });

  // AgentApi 可独立构造（无需先建 AgentLoop），供 runner 上报 workerBuildFinish / 日志
  const client = new HttpClient({ timeoutMs: config.timeoutSec * 1000 });
  const api = new AgentApi(config, client);

  // 构造 runner：把日志转发到后台构建日志接口
  const runner = new DefaultBuildRunner({
    api,
    workDir: WORK_DIR,
    workerJarPath: WORKER_JAR_PATH,
    jdk17Path: JDK17_PATH,
    jdk8Path: JDK8_PATH,
    jdk17DirPath: JDK17_DIR_PATH,
    jdk8DirPath: JDK8_DIR_PATH,
    dockerInitScriptPath: DOCKER_INIT_SCRIPT_PATH,
    gateway: config.getGateway(),
    fileGateway: config.fileGateway,
    projectId: config.projectId,
    agentVersion: AGENT_VERSION,
    workerVersion: WORKER_VERSION,
    language: config.language,
    parallelTaskCount: config.parallelTaskCount,
    dockerParallelTaskCount: config.dockerParallelTaskCount,
    // 把 runner/docker 日志上报到后台（可选）
    postLog: (build, message) => {
      void api
        .addLogLine(
          build.buildId,
          {
            message,
            timestamp: Date.now(),
            tag: 'startVM-' + build.vmSeqId,
            jobId: build.containerHashId ?? '',
            logType: LogType.Log,
            executeCount: build.executeCount,
          },
          build.vmSeqId
        )
        .catch((e) => console.error('[demo] postLog 失败', e));
    },
    logFn: (msg) => console.log('[demo][runner]', msg),
  });

  // 用带 runner 的 handler 构造 loop
  const loop = new AgentLoop({
    config,
    handler: new DemoHandler(runner),
    intervalMs: 5000,
    monitorFn: () => console.log('[demo] 采集监控指标...'),
    monitorIntervalMs: 60_000,
    // 可选：启动时准备好 worker.jar / docker init 脚本
    onInit: async () => {
      const auth = config.getAuthHeaderMap();
      try {
        const r = await downloadWorkerJar(config.getGateway(), auth, WORK_DIR);
        console.log('[demo] worker.jar 就绪 md5=', r.md5, 'notModified=', r.notModified);
      } catch (e) {
        console.error('[demo] 下载 worker.jar 失败', e);
      }
      if (ENABLE_DOCKER_BUILD) {
        try {
          const r = await downloadDockerInitFile(config.getGateway(), auth, WORK_DIR);
          console.log('[demo] docker init 脚本就绪 md5=', r.md5);
        } catch (e) {
          console.error('[demo] 下载 docker init 脚本失败', e);
        }
      }
    },
  });

  process.on('SIGINT', () => {
    console.log('收到 SIGINT，停止 agent 主循环');
    loop.stop();
  });

  await loop.run();
}

void main();

// 让 BuildJobType 被引用，避免示例中未使用告警（实际使用见 checkBuildType 逻辑）
void BuildJobType;
